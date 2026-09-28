package com.fittrack.app.data.export

import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Serialises the user's full completed workout history to a single CSV string.
 *
 * Format: one row per logged set, plus a header row. Designed to be easy to
 * import into Google Sheets / Excel and broadly compatible with the export
 * shape that Strong / FitNotes / Hevy use.
 */
class CsvExporter(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    /** Returns the CSV content. Empty body (just the header) if no workouts logged yet. */
    suspend fun export(): String {
        val sb = StringBuilder()
        sb.append(HEADER).append('\n')

        val workouts = workoutRepository.getAllWorkouts().first()
        val exercisesById = exerciseRepository.getAllExercises().first().associateBy { it.id }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        for (workoutWithExercises in workouts) {
            val workout = workoutWithExercises.workout
            val end = workout.endTime ?: continue // skip incomplete sessions
            val date = dateFormat.format(Date(workout.startTime))
            val durationMinutes = ((end - workout.startTime) / 60_000L).toString()
            val workoutNotes = csvEscape(workout.notes ?: "")

            for (we in workoutWithExercises.exercises.sortedBy { it.orderIndex }) {
                val exercise = exercisesById[we.exerciseId] ?: continue
                val exerciseName = csvEscape(exercise.name)
                val muscleGroup = exercise.muscleGroup.displayName
                val supersetCol = we.supersetGroup?.toString().orEmpty()

                val sets = workoutRepository.getSetsForWorkoutExercise(we.id).first()
                for (set in sets) {
                    sb.append(date).append(',')
                        .append(durationMinutes).append(',')
                        .append(exerciseName).append(',')
                        .append(muscleGroup).append(',')
                        .append(supersetCol).append(',')
                        .append(set.setNumber).append(',')
                        .append(if (set.isWarmup) "true" else "false").append(',')
                        .append(formatWeight(set.weightKg)).append(',')
                        .append(set.reps).append(',')
                        .append(set.rpe?.toString().orEmpty()).append(',')
                        .append(workoutNotes)
                        .append('\n')
                }
            }
        }

        return sb.toString()
    }

    companion object {
        private const val HEADER =
            "Date,Duration (min),Exercise,Muscle Group,Superset,Set,Warmup,Weight (kg),Reps,RPE,Notes"
    }
}

/**
 * Wrap a value in quotes and escape inner quotes per RFC 4180.
 * Cheap path: no escape needed if the value has no commas, quotes, or newlines.
 */
private fun csvEscape(value: String): String {
    if (value.isEmpty()) return ""
    val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
    if (!needsQuoting) return value
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}

/**
 * Drop the trailing `.0` for whole numbers; otherwise emit at most two
 * decimals with no trailing zeros — keeps the spreadsheet view tidy.
 */
private fun formatWeight(weight: Double): String {
    if (weight == weight.toLong().toDouble()) return weight.toLong().toString()
    return "%.2f".format(weight).trimEnd('0').trimEnd('.')
}
