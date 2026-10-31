package com.fittrack.app.sync

import android.util.Log
import com.fittrack.app.data.db.dao.SyncDao
import com.fittrack.app.data.entity.Equipment
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.data.entity.Workout
import com.fittrack.app.data.entity.WorkoutExercise
import com.fittrack.app.data.entity.WorkoutSet
import com.fittrack.app.data.repository.ExerciseRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Push dirty rows and tombstones, then pull everything newer than our cursor.
 *
 * Ordering: parents before children on push (exercises → workouts →
 * workout_exercises → sets) so foreign references always resolve, and the
 * same order on pull. Conflicts: a pulled row only replaces the local one
 * when its updated_at is newer. Deleted rows pulled as tombstones are removed
 * locally. Serialized with a mutex so WorkManager and foreground triggers
 * never overlap.
 */
class SyncEngine(
    private val syncDao: SyncDao,
    private val exerciseRepository: ExerciseRepository,
    private val prefs: SyncPrefs,
    private val account: AccountManager,
) {
    private val lock = Mutex()

    /** Returns null on success or a short error message. */
    suspend fun syncNow(): String? = lock.withLock {
        val client = SupabaseProvider.client ?: return "Sync isn't configured in this build."
        val user = client.auth.currentUserOrNull() ?: return "Not signed in."
        if (prefs.userId != user.id) {
            // Different account than last time: start the cursor over so this
            // account's full history is pulled.
            prefs.reset()
            prefs.userId = user.id
        }
        prefs.setStatus(SyncStatus.Syncing)
        return try {
            assignMissingIds()
            push(client)
            pull(client)
            prefs.lastSyncedAt = System.currentTimeMillis()
            prefs.setStatus(SyncStatus.Idle)
            null
        } catch (t: Throwable) {
            Log.w("SyncEngine", "sync failed", t)
            val msg = t.message?.take(140) ?: "Sync failed"
            prefs.setStatus(SyncStatus.Error(msg))
            msg
        }
    }

    // ------------------------------------------------------------------ ids

    /** Rows created before sync existed get ids now; catalog exercises get deterministic ones. */
    private suspend fun assignMissingIds() {
        val now = System.currentTimeMillis()
        syncDao.exercisesWithoutRemoteId().forEach { e ->
            syncDao.setExerciseRemoteId(e.id, if (e.isCustom) newId() else catalogId(e.name))
        }
        syncDao.workoutsWithoutRemoteId().forEach { syncDao.setWorkoutRemoteId(it.id, newId(), now) }
        syncDao.workoutExercisesWithoutRemoteId().forEach { syncDao.setWorkoutExerciseRemoteId(it.id, newId(), now) }
        syncDao.setsWithoutRemoteId().forEach { syncDao.setSetRemoteId(it.id, newId(), now) }
    }

    // ----------------------------------------------------------------- push

    private suspend fun push(client: io.github.jan.supabase.SupabaseClient) {
        val now = System.currentTimeMillis()

        val exercises = syncDao.dirtyCustomExercises()
        if (exercises.isNotEmpty()) {
            client.from("exercises").upsert(exercises.map { e ->
                ExerciseDto(
                    id = e.remoteId!!, updated_at = e.updatedAt ?: now, deleted = false,
                    name = e.name, muscle_group = e.muscleGroup.name,
                    secondary_muscle_group = e.secondaryMuscleGroup?.name, equipment = e.equipment.name,
                )
            })
            syncDao.cleanExercises(exercises.map { it.id })
        }

        val workouts = syncDao.dirtyWorkouts()
        if (workouts.isNotEmpty()) {
            client.from("workouts").upsert(workouts.map { w ->
                WorkoutDto(id = w.remoteId!!, updated_at = w.updatedAt ?: now, deleted = false,
                    start_time = w.startTime, end_time = w.endTime, notes = w.notes)
            })
            syncDao.cleanWorkouts(workouts.map { it.id })
        }

        val wes = syncDao.dirtyWorkoutExercises()
        if (wes.isNotEmpty()) {
            val dtos = wes.mapNotNull { we ->
                val workout = syncDao.workoutByRemoteIdOrLocal(we.workoutId) ?: return@mapNotNull null
                val exercise = exerciseRepository.getById(we.exerciseId) ?: return@mapNotNull null
                WorkoutExerciseDto(
                    id = we.remoteId!!, updated_at = we.updatedAt ?: now, deleted = false,
                    workout_id = workout.remoteId!!,
                    exercise_ref = exercise.remoteId ?: catalogId(exercise.name),
                    order_index = we.orderIndex,
                    superset_group = we.supersetGroup?.toString(),
                )
            }
            if (dtos.isNotEmpty()) client.from("workout_exercises").upsert(dtos)
            syncDao.cleanWorkoutExercises(wes.map { it.id })
        }

        val sets = syncDao.dirtySets()
        if (sets.isNotEmpty()) {
            val dtos = sets.mapNotNull { s ->
                val we = syncDao.workoutExerciseByLocalId(s.workoutExerciseId) ?: return@mapNotNull null
                WorkoutSetDto(
                    id = s.remoteId!!, updated_at = s.updatedAt ?: now, deleted = false,
                    workout_exercise_id = we.remoteId!!, set_number = s.setNumber, reps = s.reps,
                    weight_kg = s.weightKg, is_warmup = s.isWarmup, rpe = s.rpe,
                )
            }
            if (dtos.isNotEmpty()) client.from("workout_sets").upsert(dtos)
            syncDao.cleanSets(sets.map { it.id })
        }

        val tombstones = syncDao.tombstones()
        for (t in tombstones) {
            client.from(t.tableName).update({ set("deleted", true); set("updated_at", t.deletedAt) }) {
                filter { eq("id", t.remoteId) }
            }
        }
        if (tombstones.isNotEmpty()) syncDao.clearTombstones(tombstones.map { it.id })
    }

    // ----------------------------------------------------------------- pull

    private suspend fun pull(client: io.github.jan.supabase.SupabaseClient) {
        var cursor = prefs.cursor
        var advanced = true
        while (advanced) {
            advanced = false
            val page = 500L
            val ex = client.from("exercises").select { filter { gt("seq", cursor) }; order("seq", Order.ASCENDING); limit(page) }.decodeList<ExerciseDto>()
            val wo = client.from("workouts").select { filter { gt("seq", cursor) }; order("seq", Order.ASCENDING); limit(page) }.decodeList<WorkoutDto>()
            val we = client.from("workout_exercises").select { filter { gt("seq", cursor) }; order("seq", Order.ASCENDING); limit(page) }.decodeList<WorkoutExerciseDto>()
            val ws = client.from("workout_sets").select { filter { gt("seq", cursor) }; order("seq", Order.ASCENDING); limit(page) }.decodeList<WorkoutSetDto>()
            if (ex.isEmpty() && wo.isEmpty() && we.isEmpty() && ws.isEmpty()) break

            ex.forEach { applyExercise(it) }
            wo.forEach { applyWorkout(it) }
            we.forEach { applyWorkoutExercise(it) }
            ws.forEach { applySet(it) }

            val maxSeq = listOf(ex.maxOfOrNull { it.seq ?: 0 }, wo.maxOfOrNull { it.seq ?: 0 },
                we.maxOfOrNull { it.seq ?: 0 }, ws.maxOfOrNull { it.seq ?: 0 }).filterNotNull().maxOrNull() ?: cursor
            if (maxSeq > cursor) {
                cursor = maxSeq
                prefs.cursor = cursor
                advanced = ex.size.toLong() == page || wo.size.toLong() == page || we.size.toLong() == page || ws.size.toLong() == page
            }
        }
    }

    private suspend fun applyExercise(d: ExerciseDto) {
        val local = syncDao.exerciseByRemoteId(d.id)
        if (d.deleted) { local?.let { syncDao.deleteCustomExerciseLocal(it.id) }; return }
        val incoming = Exercise(
            id = local?.id ?: 0, name = d.name,
            muscleGroup = enumOr(d.muscle_group, MuscleGroup.FULL_BODY),
            secondaryMuscleGroup = d.secondary_muscle_group?.let { enumOr(it, MuscleGroup.FULL_BODY) },
            isCustom = true, equipment = enumOr(d.equipment, Equipment.OTHER),
            remoteId = d.id, updatedAt = d.updated_at, dirty = false,
        )
        if (local == null) syncDao.insertExercise(incoming)
        else if ((local.updatedAt ?: 0) < d.updated_at) syncDao.updateExercise(incoming)
    }

    private suspend fun applyWorkout(d: WorkoutDto) {
        val local = syncDao.workoutByRemoteId(d.id)
        if (d.deleted) { local?.let { syncDao.deleteWorkoutLocal(it.id) }; return }
        val incoming = Workout(id = local?.id ?: 0, startTime = d.start_time, endTime = d.end_time, notes = d.notes,
            remoteId = d.id, updatedAt = d.updated_at, dirty = false)
        if (local == null) syncDao.insertWorkout(incoming)
        else if ((local.updatedAt ?: 0) < d.updated_at) syncDao.updateWorkout(incoming)
    }

    private suspend fun applyWorkoutExercise(d: WorkoutExerciseDto) {
        val local = syncDao.workoutExerciseByRemoteId(d.id)
        if (d.deleted) { local?.let { syncDao.deleteWorkoutExerciseLocal(it.id) }; return }
        val workout = syncDao.workoutByRemoteId(d.workout_id) ?: return
        val exercise = resolveExercise(d.exercise_ref) ?: return
        val incoming = WorkoutExercise(id = local?.id ?: 0, workoutId = workout.id, exerciseId = exercise.id,
            orderIndex = d.order_index, supersetGroup = d.superset_group?.toLongOrNull(),
            remoteId = d.id, updatedAt = d.updated_at, dirty = false)
        if (local == null) syncDao.insertWorkoutExercise(incoming)
        else if ((local.updatedAt ?: 0) < d.updated_at) syncDao.updateWorkoutExercise(incoming)
    }

    private suspend fun applySet(d: WorkoutSetDto) {
        val local = syncDao.setByRemoteId(d.id)
        if (d.deleted) { local?.let { syncDao.deleteSetLocal(it.id) }; return }
        val we = syncDao.workoutExerciseByRemoteId(d.workout_exercise_id) ?: return
        val incoming = WorkoutSet(id = local?.id ?: 0, workoutExerciseId = we.id, setNumber = d.set_number,
            reps = d.reps, weightKg = d.weight_kg, isWarmup = d.is_warmup, rpe = d.rpe,
            remoteId = d.id, updatedAt = d.updated_at, dirty = false)
        if (local == null) syncDao.insertSet(incoming)
        else if ((local.updatedAt ?: 0) < d.updated_at) syncDao.updateSet(incoming)
    }

    /** Catalog refs resolve by normalized name; custom refs by remote id. */
    private suspend fun resolveExercise(ref: String): Exercise? {
        syncDao.exerciseByRemoteId(ref)?.let { return it }
        if (ref.startsWith("catalog:")) {
            val norm = ref.removePrefix("catalog:")
            return exerciseRepository.getAllExercises().first().firstOrNull { normalize(it.name) == norm }
        }
        return null
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
        runCatching { enumValueOf<E>(name) }.getOrDefault(default)

    private fun newId() = java.util.UUID.randomUUID().toString()

    companion object {
        fun normalize(name: String) = name.lowercase().filter { it.isLetterOrDigit() }
        fun catalogId(name: String) = "catalog:" + normalize(name)
    }
}

// ---------------------------------------------------------------- DTOs (snake_case = server columns)

@Serializable
data class ExerciseDto(
    val id: String, val seq: Long? = null, val updated_at: Long, val deleted: Boolean = false,
    val name: String, val muscle_group: String, val secondary_muscle_group: String? = null, val equipment: String,
)

@Serializable
data class WorkoutDto(
    val id: String, val seq: Long? = null, val updated_at: Long, val deleted: Boolean = false,
    val start_time: Long, val end_time: Long? = null, val notes: String? = null,
)

@Serializable
data class WorkoutExerciseDto(
    val id: String, val seq: Long? = null, val updated_at: Long, val deleted: Boolean = false,
    val workout_id: String, val exercise_ref: String, val order_index: Int, val superset_group: String? = null,
)

@Serializable
data class WorkoutSetDto(
    val id: String, val seq: Long? = null, val updated_at: Long, val deleted: Boolean = false,
    val workout_exercise_id: String, val set_number: Int, val reps: Int, val weight_kg: Double,
    val is_warmup: Boolean = false, val rpe: Int? = null,
)
