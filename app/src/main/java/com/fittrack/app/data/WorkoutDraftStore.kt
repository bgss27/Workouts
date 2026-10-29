package com.fittrack.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Crash / process-death insurance for the in-progress workout.
 *
 * Sets are only written to Room when the user taps Finish, so if Android
 * kills the app while the phone is locked between sets, everything typed so
 * far would vanish. The active-workout view model mirrors its UI state here
 * (debounced) after every change and restores it on next launch when the
 * matching workout row is still open (endTime == null).
 */
class WorkoutDraftStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class DraftSet(
        val reps: String,
        val weight: String,
        val isWarmup: Boolean,
        val rpe: Int?,
        val isCompleted: Boolean,
    )

    data class DraftExercise(
        val workoutExerciseId: Long,
        val exerciseId: Long,
        val supersetGroup: Long?,
        val sets: List<DraftSet>,
    )

    data class Draft(
        val workoutId: Long,
        val weightUnit: String,
        val exercises: List<DraftExercise>,
    )

    fun save(draft: Draft) {
        val root = JSONObject()
            .put("workoutId", draft.workoutId)
            .put("weightUnit", draft.weightUnit)
            .put("exercises", JSONArray().also { arr ->
                draft.exercises.forEach { ex ->
                    arr.put(
                        JSONObject()
                            .put("workoutExerciseId", ex.workoutExerciseId)
                            .put("exerciseId", ex.exerciseId)
                            .put("supersetGroup", ex.supersetGroup ?: JSONObject.NULL)
                            .put("sets", JSONArray().also { sets ->
                                ex.sets.forEach { s ->
                                    sets.put(
                                        JSONObject()
                                            .put("reps", s.reps)
                                            .put("weight", s.weight)
                                            .put("isWarmup", s.isWarmup)
                                            .put("rpe", s.rpe ?: JSONObject.NULL)
                                            .put("isCompleted", s.isCompleted)
                                    )
                                }
                            })
                    )
                }
            })
        prefs.edit().putString(KEY_DRAFT, root.toString()).apply()
    }

    fun load(): Draft? {
        val raw = prefs.getString(KEY_DRAFT, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val exercises = root.getJSONArray("exercises").let { arr ->
                (0 until arr.length()).map { i ->
                    val ex = arr.getJSONObject(i)
                    DraftExercise(
                        workoutExerciseId = ex.getLong("workoutExerciseId"),
                        exerciseId = ex.getLong("exerciseId"),
                        supersetGroup = if (ex.isNull("supersetGroup")) null else ex.getLong("supersetGroup"),
                        sets = ex.getJSONArray("sets").let { sets ->
                            (0 until sets.length()).map { j ->
                                val s = sets.getJSONObject(j)
                                DraftSet(
                                    reps = s.optString("reps", ""),
                                    weight = s.optString("weight", ""),
                                    isWarmup = s.optBoolean("isWarmup", false),
                                    rpe = if (s.isNull("rpe")) null else s.getInt("rpe"),
                                    isCompleted = s.optBoolean("isCompleted", false),
                                )
                            }
                        },
                    )
                }
            }
            Draft(
                workoutId = root.getLong("workoutId"),
                weightUnit = root.optString("weightUnit", "kg"),
                exercises = exercises,
            )
        }.getOrNull()
    }

    fun hasDraft(): Boolean = prefs.contains(KEY_DRAFT)

    fun clear() {
        prefs.edit().remove(KEY_DRAFT).apply()
    }

    private companion object {
        const val PREFS = "fittrack_workout_draft"
        const val KEY_DRAFT = "draft"
    }
}
