package com.fittrack.app.domain.analysis

import com.fittrack.app.data.entity.MuscleGroup

/**
 * A single warmup or mobility movement. [prescription] is the human-readable
 * volume cue ("2 × 10 reps", "30 seconds"); [note] is an optional one-liner
 * shown in smaller text underneath.
 */
data class WarmupExercise(
    val name: String,
    val prescription: String,
    val note: String? = null,
)

/** Group of warmups shown together (general dynamic warmup or per-muscle activation). */
data class WarmupSection(
    val title: String,
    val exercises: List<WarmupExercise>,
)

/**
 * Surfaces concrete warmup exercises tailored to the muscle groups being
 * trained. The previous `TimeOfDayAdvisor` only said *how much* to warm up
 * (mins / number of sets); this fills the gap by saying *what* to do.
 *
 * Catalog stays small and opinionated on purpose — three exercises per
 * muscle group, five for the general dynamic warmup. Long lists discourage
 * the user from actually doing them.
 */
object WarmupCatalog {

    /** Cap on per-muscle activation sections shown. Keeps the banner short. */
    private const val MAX_ACTIVATION_SECTIONS = 3

    private val general = listOf(
        WarmupExercise("Arm circles", "30s forward, 30s backward"),
        WarmupExercise("Leg swings", "10 each side, front/back & lateral"),
        WarmupExercise("Hip circles", "10 each direction"),
        WarmupExercise("Bodyweight squats", "10 reps", note = "Slow and controlled"),
        WarmupExercise("Cat-cow stretches", "10 reps"),
    )

    private val byMuscleGroup: Map<MuscleGroup, List<WarmupExercise>> = mapOf(
        MuscleGroup.CHEST to listOf(
            WarmupExercise("Scapular push-ups", "2 × 10 reps"),
            WarmupExercise("Band pull-aparts", "2 × 15 reps"),
            WarmupExercise("Wall slides", "2 × 10 reps"),
        ),
        MuscleGroup.BACK to listOf(
            WarmupExercise("Dead hangs", "2 × 15 seconds"),
            WarmupExercise("Scapular pulls", "2 × 10 reps", note = "Hang and pull shoulders down"),
            WarmupExercise("Band rows", "2 × 15 reps"),
        ),
        MuscleGroup.SHOULDERS to listOf(
            WarmupExercise("Band shoulder dislocates", "2 × 10 reps", note = "Or broomstick / PVC"),
            WarmupExercise("Wall slides", "2 × 10 reps"),
            WarmupExercise("Empty-bar overhead press", "2 × 10 reps"),
        ),
        MuscleGroup.BICEPS to listOf(
            WarmupExercise("Light dumbbell curls", "2 × 10 reps", note = "20–30% of working weight"),
            WarmupExercise("Band curls", "2 × 15 reps"),
        ),
        MuscleGroup.TRICEPS to listOf(
            WarmupExercise("Light tricep pushdowns", "2 × 10 reps", note = "30–40% of working weight"),
            WarmupExercise("Band pushdowns", "2 × 15 reps"),
            WarmupExercise("Diamond push-ups (knees)", "2 × 10 reps"),
        ),
        MuscleGroup.LEGS to listOf(
            WarmupExercise("Bodyweight squats", "2 × 10 reps"),
            WarmupExercise("Walking lunges", "10 steps per leg"),
            WarmupExercise("Goblet squats (light)", "2 × 10 reps", note = "Light dumbbell or kettlebell"),
            WarmupExercise("Leg swings", "10 each direction"),
        ),
        MuscleGroup.GLUTES to listOf(
            WarmupExercise("Glute bridges", "2 × 15 reps"),
            WarmupExercise("Clamshells", "2 × 10 per side"),
            WarmupExercise("Band lateral walks", "10 steps per side"),
        ),
        MuscleGroup.ABS to listOf(
            WarmupExercise("Plank", "2 × 30 seconds"),
            WarmupExercise("Dead bug", "2 × 10 reps"),
            WarmupExercise("Cat-cow", "10 reps"),
        ),
        MuscleGroup.CALVES to listOf(
            WarmupExercise("Bodyweight calf raises", "2 × 15 reps"),
            WarmupExercise("Ankle circles", "10 each direction"),
        ),
        MuscleGroup.FOREARMS to listOf(
            WarmupExercise("Wrist circles", "10 each direction"),
            WarmupExercise("Light grip squeezes", "30 seconds"),
        ),
    )

    /**
     * Build the warmup checklist for a workout that targets [groups]. The
     * general dynamic warmup is always included; per-muscle activation
     * follows for up to [MAX_ACTIVATION_SECTIONS] groups.
     *
     * Order in [groups] is preserved (so the workout's primary movers get
     * their activation listed first). Duplicates are filtered out.
     */
    fun forMuscleGroups(groups: Collection<MuscleGroup>): List<WarmupSection> {
        val sections = mutableListOf(
            WarmupSection(title = "Dynamic warmup · ~5 min", exercises = general)
        )
        val unique = groups.toMutableList().distinct().take(MAX_ACTIVATION_SECTIONS)
        for (group in unique) {
            byMuscleGroup[group]?.let { exercises ->
                sections += WarmupSection(
                    title = "Activation · ${group.displayName}",
                    exercises = exercises,
                )
            }
        }
        return sections
    }
}
