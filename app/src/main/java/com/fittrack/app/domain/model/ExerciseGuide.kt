package com.fittrack.app.domain.model

enum class MovementPattern(val label: String, val description: String) {
    HORIZONTAL_PUSH("Horizontal Push", "Push weight away from chest horizontally"),
    HORIZONTAL_PULL("Horizontal Pull", "Pull weight toward chest horizontally"),
    VERTICAL_PUSH("Vertical Push", "Press weight overhead"),
    VERTICAL_PULL("Vertical Pull", "Pull weight down from above"),
    SQUAT("Squat", "Lower body by bending knees and hips"),
    HIP_HINGE("Hip Hinge", "Bend at hips keeping back straight"),
    CURL("Curl", "Flex at elbow bringing weight toward shoulder"),
    EXTENSION("Extension", "Extend at elbow or knee"),
    LATERAL_RAISE("Lateral Raise", "Raise arms out to the sides"),
    ISOLATION("Isolation", "Targeted single-joint movement"),
    STATIC_HOLD("Static Hold", "Hold position under tension"),
    CARRY("Carry", "Walk while holding weight")
}

data class ExerciseGuide(
    val steps: List<String>,
    val breathingCue: String,
    val commonMistakes: List<String>,
    val movementPattern: MovementPattern,
    val tempo: String // e.g. "2-1-2" (eccentric-pause-concentric seconds)
)

object ExerciseGuideData {
    private val guides = mapOf(
        // Chest
        "Barbell Bench Press" to ExerciseGuide(
            steps = listOf("Lie flat on bench, feet on floor, grip bar slightly wider than shoulders", "Unrack bar, lower to mid-chest with elbows at ~45°", "Touch chest lightly, press bar up and slightly back", "Lock out arms at the top, repeat"),
            breathingCue = "Inhale on the way down, exhale as you press up",
            commonMistakes = listOf("Flaring elbows to 90°", "Bouncing bar off chest", "Lifting hips off bench", "Not retracting shoulder blades"),
            movementPattern = MovementPattern.HORIZONTAL_PUSH, tempo = "2-1-2"
        ),
        "Incline Dumbbell Press" to ExerciseGuide(
            steps = listOf("Set bench to 30-45° incline", "Hold dumbbells at shoulder height, palms forward", "Press up and slightly inward until arms are extended", "Lower slowly to shoulder level"),
            breathingCue = "Inhale down, exhale up",
            commonMistakes = listOf("Setting incline too steep (>45°)", "Not controlling the descent", "Dumbbells drifting too far forward"),
            movementPattern = MovementPattern.HORIZONTAL_PUSH, tempo = "2-1-2"
        ),
        "Dumbbell Bench Press" to ExerciseGuide(
            steps = listOf("Lie flat, hold dumbbells at chest level", "Press up until arms are extended", "Lower with control back to chest level"),
            breathingCue = "Inhale down, exhale up",
            commonMistakes = listOf("Dumbbells too wide at bottom", "Uneven pressing", "Not going to full range"),
            movementPattern = MovementPattern.HORIZONTAL_PUSH, tempo = "2-1-2"
        ),
        "Cable Fly" to ExerciseGuide(
            steps = listOf("Set cables at chest height, step forward", "With slight elbow bend, bring handles together in front", "Squeeze chest at the center, return slowly"),
            breathingCue = "Exhale as you bring handles together",
            commonMistakes = listOf("Bending elbows too much (turns into a press)", "Using momentum", "Leaning too far forward"),
            movementPattern = MovementPattern.ISOLATION, tempo = "2-1-3"
        ),
        "Push-Up" to ExerciseGuide(
            steps = listOf("Hands shoulder-width, body in straight line", "Lower chest to floor, elbows at 45°", "Push back up to full arm extension"),
            breathingCue = "Inhale down, exhale up",
            commonMistakes = listOf("Sagging hips", "Flaring elbows", "Not reaching full range"),
            movementPattern = MovementPattern.HORIZONTAL_PUSH, tempo = "2-0-1"
        ),
        // Back
        "Barbell Row" to ExerciseGuide(
            steps = listOf("Hinge at hips ~45°, grip bar outside knees", "Pull bar to lower chest/upper abdomen", "Squeeze shoulder blades together at top", "Lower with control"),
            breathingCue = "Exhale as you pull, inhale as you lower",
            commonMistakes = listOf("Rounding lower back", "Using momentum to swing bar", "Standing too upright", "Not squeezing at the top"),
            movementPattern = MovementPattern.HORIZONTAL_PULL, tempo = "1-1-2"
        ),
        "Pull-Up" to ExerciseGuide(
            steps = listOf("Hang from bar with overhand grip, slightly wider than shoulders", "Pull body up until chin clears bar", "Lower with control to full hang"),
            breathingCue = "Exhale pulling up, inhale lowering",
            commonMistakes = listOf("Kipping or swinging", "Not reaching full extension at bottom", "Chin not clearing bar"),
            movementPattern = MovementPattern.VERTICAL_PULL, tempo = "1-1-3"
        ),
        "Lat Pulldown" to ExerciseGuide(
            steps = listOf("Grip bar wide, sit with thighs under pad", "Pull bar to upper chest, leaning slightly back", "Squeeze lats, return bar slowly overhead"),
            breathingCue = "Exhale pulling down, inhale releasing up",
            commonMistakes = listOf("Pulling behind neck", "Leaning too far back", "Using momentum"),
            movementPattern = MovementPattern.VERTICAL_PULL, tempo = "1-1-3"
        ),
        "Deadlift" to ExerciseGuide(
            steps = listOf("Stand with feet hip-width, bar over mid-foot", "Hinge at hips, grip bar just outside knees", "Drive through heels, extend hips and knees together", "Stand tall, squeeze glutes at top", "Lower bar by hinging at hips first"),
            breathingCue = "Big breath and brace before lifting, exhale at top",
            commonMistakes = listOf("Rounding lower back", "Bar drifting from body", "Jerking the weight", "Hyperextending at the top"),
            movementPattern = MovementPattern.HIP_HINGE, tempo = "1-0-3"
        ),
        // Shoulders
        "Overhead Press" to ExerciseGuide(
            steps = listOf("Stand with bar at front shoulders, grip just outside shoulders", "Press bar overhead in slight arc, moving head through", "Lock out arms directly overhead", "Lower with control to shoulders"),
            breathingCue = "Inhale at bottom, exhale pressing up",
            commonMistakes = listOf("Excessive back arch", "Pressing forward instead of overhead", "Not locking out fully"),
            movementPattern = MovementPattern.VERTICAL_PUSH, tempo = "1-1-2"
        ),
        "Lateral Raise" to ExerciseGuide(
            steps = listOf("Stand with dumbbells at sides, slight bend in elbows", "Raise arms out to sides until parallel to floor", "Pause briefly, lower slowly"),
            breathingCue = "Exhale raising, inhale lowering",
            commonMistakes = listOf("Using too much weight and swinging", "Raising above shoulder level", "Shrugging shoulders up", "Leading with thumbs up (should be pinkies slightly up)"),
            movementPattern = MovementPattern.LATERAL_RAISE, tempo = "2-1-3"
        ),
        // Biceps
        "Barbell Curl" to ExerciseGuide(
            steps = listOf("Stand with bar at hip level, shoulder-width underhand grip", "Curl bar up by flexing elbows, keep upper arms still", "Squeeze biceps at top, lower slowly"),
            breathingCue = "Exhale curling up, inhale lowering",
            commonMistakes = listOf("Swinging body for momentum", "Moving elbows forward", "Not controlling the negative"),
            movementPattern = MovementPattern.CURL, tempo = "2-1-3"
        ),
        "Hammer Curl" to ExerciseGuide(
            steps = listOf("Hold dumbbells at sides with neutral grip (palms facing in)", "Curl up keeping neutral grip throughout", "Lower with control"),
            breathingCue = "Exhale up, inhale down",
            commonMistakes = listOf("Swinging body", "Rotating wrists during curl"),
            movementPattern = MovementPattern.CURL, tempo = "2-1-3"
        ),
        // Triceps
        "Tricep Pushdown" to ExerciseGuide(
            steps = listOf("Face cable machine, grip bar with overhand grip", "Keep upper arms tight to body, push bar down", "Fully extend arms, squeeze triceps", "Return with control, don't let elbows drift forward"),
            breathingCue = "Exhale pushing down, inhale releasing",
            commonMistakes = listOf("Elbows flaring out", "Leaning over the bar", "Using momentum"),
            movementPattern = MovementPattern.EXTENSION, tempo = "1-1-3"
        ),
        "Skull Crushers" to ExerciseGuide(
            steps = listOf("Lie on bench, hold bar above forehead with narrow grip", "Bend elbows to lower bar toward forehead/behind head", "Extend arms back to start, squeeze triceps"),
            breathingCue = "Inhale lowering, exhale extending",
            commonMistakes = listOf("Elbows flaring wide", "Moving upper arms", "Lowering to face instead of forehead"),
            movementPattern = MovementPattern.EXTENSION, tempo = "2-0-2"
        ),
        // Legs
        "Barbell Squat" to ExerciseGuide(
            steps = listOf("Bar on upper traps, feet shoulder-width, toes slightly out", "Brace core, break at hips and knees simultaneously", "Descend until thighs are parallel (or below)", "Drive up through heels, keeping chest up"),
            breathingCue = "Big breath and brace before descending, exhale standing up",
            commonMistakes = listOf("Knees caving inward", "Rounding lower back", "Not hitting depth", "Weight shifting to toes", "Excessive forward lean"),
            movementPattern = MovementPattern.SQUAT, tempo = "2-1-2"
        ),
        "Leg Press" to ExerciseGuide(
            steps = listOf("Sit in machine with feet shoulder-width on platform", "Release safety, lower platform by bending knees", "Lower until knees are at 90° (don't let lower back round)", "Press back up without locking knees"),
            breathingCue = "Inhale lowering, exhale pressing",
            commonMistakes = listOf("Going too deep (lower back lifts off pad)", "Locking knees at top", "Feet too high or low on platform"),
            movementPattern = MovementPattern.SQUAT, tempo = "2-1-2"
        ),
        "Romanian Deadlift" to ExerciseGuide(
            steps = listOf("Hold bar at hip level, feet hip-width", "Push hips back, lower bar along legs", "Keep slight knee bend, feel stretch in hamstrings", "Drive hips forward to stand, squeeze glutes"),
            breathingCue = "Inhale on the way down, exhale driving hips forward",
            commonMistakes = listOf("Rounding lower back", "Bending knees too much", "Bar drifting from legs", "Not pushing hips back far enough"),
            movementPattern = MovementPattern.HIP_HINGE, tempo = "3-0-2"
        ),
        "Hip Thrust" to ExerciseGuide(
            steps = listOf("Upper back on bench, bar across hips with pad", "Feet flat on floor, shoulder-width", "Drive hips up by squeezing glutes until body is straight", "Pause at top, lower with control"),
            breathingCue = "Exhale thrusting up, inhale lowering",
            commonMistakes = listOf("Hyperextending lower back at top", "Feet too far from body", "Not pausing at top"),
            movementPattern = MovementPattern.HIP_HINGE, tempo = "1-2-2"
        ),
        // Abs
        "Plank" to ExerciseGuide(
            steps = listOf("Forearms and toes on floor, body in straight line", "Brace core tightly, squeeze glutes", "Hold position without sagging or piking"),
            breathingCue = "Breathe normally while maintaining brace",
            commonMistakes = listOf("Sagging hips", "Piking hips up", "Holding breath", "Looking up (keep neck neutral)"),
            movementPattern = MovementPattern.STATIC_HOLD, tempo = "hold"
        ),
        "Hanging Leg Raise" to ExerciseGuide(
            steps = listOf("Hang from bar with straight arms", "Raise legs by flexing at hips and curling pelvis", "Lift until legs are parallel or higher", "Lower slowly with control"),
            breathingCue = "Exhale raising legs, inhale lowering",
            commonMistakes = listOf("Swinging body", "Using hip flexors only (not curling pelvis)", "Not controlling the descent"),
            movementPattern = MovementPattern.CURL, tempo = "2-1-3"
        ),
        // Calves
        "Standing Calf Raise" to ExerciseGuide(
            steps = listOf("Stand on platform edge, heels hanging off", "Rise up onto toes as high as possible", "Pause and squeeze at top", "Lower slowly below platform level for full stretch"),
            breathingCue = "Exhale rising, inhale lowering",
            commonMistakes = listOf("Bouncing at bottom", "Not going through full range", "Bending knees"),
            movementPattern = MovementPattern.EXTENSION, tempo = "2-2-3"
        ),
        "Farmer's Walk" to ExerciseGuide(
            steps = listOf("Pick up heavy dumbbells/kettlebells at sides", "Stand tall, shoulders back, core braced", "Walk with short controlled steps", "Maintain upright posture throughout"),
            breathingCue = "Breathe steadily, keep core braced",
            commonMistakes = listOf("Leaning to one side", "Shoulders rounding forward", "Taking too-long steps"),
            movementPattern = MovementPattern.CARRY, tempo = "walk"
        )
    )

    fun getGuide(exerciseName: String): ExerciseGuide {
        return guides[exerciseName] ?: defaultGuide(exerciseName)
    }

    private fun defaultGuide(name: String): ExerciseGuide {
        return ExerciseGuide(
            steps = listOf("Set up with proper form", "Perform the movement with control", "Return to start position"),
            breathingCue = "Exhale during exertion, inhale during the return",
            commonMistakes = listOf("Using too much weight", "Not controlling the movement"),
            movementPattern = MovementPattern.ISOLATION,
            tempo = "2-1-2"
        )
    }
}
