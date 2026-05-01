import Foundation

enum MovementPattern: String {
    case horizontalPush = "Horizontal Push"
    case horizontalPull = "Horizontal Pull"
    case verticalPush = "Vertical Push"
    case verticalPull = "Vertical Pull"
    case squat = "Squat"
    case hipHinge = "Hip Hinge"
    case curl = "Curl"
    case ext = "Extension"
    case lateralRaise = "Lateral Raise"
    case isolation = "Isolation"
    case staticHold = "Static Hold"
    case carry = "Carry"
}

struct ExerciseGuide {
    let steps: [String]
    let breathingCue: String
    let commonMistakes: [String]
    let movementPattern: MovementPattern
    let tempo: String
}

struct ExerciseGuideData {
    static func getGuide(_ name: String) -> ExerciseGuide {
        guides[name] ?? defaultGuide()
    }

    private static func defaultGuide() -> ExerciseGuide {
        ExerciseGuide(
            steps: ["Set up with proper form", "Perform the movement with control", "Return to start position"],
            breathingCue: "Exhale during exertion, inhale during the return",
            commonMistakes: ["Using too much weight", "Not controlling the movement"],
            movementPattern: .isolation, tempo: "2-1-2"
        )
    }

    private static let guides: [String: ExerciseGuide] = [
        "Barbell Bench Press": ExerciseGuide(
            steps: ["Lie flat on bench, feet on floor, grip bar slightly wider than shoulders", "Unrack bar, lower to mid-chest with elbows at ~45°", "Touch chest lightly, press bar up and slightly back", "Lock out arms at the top, repeat"],
            breathingCue: "Inhale on the way down, exhale as you press up",
            commonMistakes: ["Flaring elbows to 90°", "Bouncing bar off chest", "Lifting hips off bench", "Not retracting shoulder blades"],
            movementPattern: .horizontalPush, tempo: "2-1-2"
        ),
        "Incline Dumbbell Press": ExerciseGuide(
            steps: ["Set bench to 30-45° incline", "Hold dumbbells at shoulder height, palms forward", "Press up and slightly inward until arms are extended", "Lower slowly to shoulder level"],
            breathingCue: "Inhale down, exhale up",
            commonMistakes: ["Setting incline too steep (>45°)", "Not controlling the descent", "Dumbbells drifting too far forward"],
            movementPattern: .horizontalPush, tempo: "2-1-2"
        ),
        "Barbell Row": ExerciseGuide(
            steps: ["Hinge at hips ~45°, grip bar outside knees", "Pull bar to lower chest/upper abdomen", "Squeeze shoulder blades together at top", "Lower with control"],
            breathingCue: "Exhale as you pull, inhale as you lower",
            commonMistakes: ["Rounding lower back", "Using momentum to swing bar", "Standing too upright"],
            movementPattern: .horizontalPull, tempo: "1-1-2"
        ),
        "Pull-Up": ExerciseGuide(
            steps: ["Hang from bar with overhand grip, slightly wider than shoulders", "Pull body up until chin clears bar", "Lower with control to full hang"],
            breathingCue: "Exhale pulling up, inhale lowering",
            commonMistakes: ["Kipping or swinging", "Not reaching full extension at bottom", "Chin not clearing bar"],
            movementPattern: .verticalPull, tempo: "1-1-3"
        ),
        "Deadlift": ExerciseGuide(
            steps: ["Stand with feet hip-width, bar over mid-foot", "Hinge at hips, grip bar just outside knees", "Drive through heels, extend hips and knees together", "Stand tall, squeeze glutes at top", "Lower by hinging at hips first"],
            breathingCue: "Big breath and brace before lifting, exhale at top",
            commonMistakes: ["Rounding lower back", "Bar drifting from body", "Jerking the weight", "Hyperextending at the top"],
            movementPattern: .hipHinge, tempo: "1-0-3"
        ),
        "Overhead Press": ExerciseGuide(
            steps: ["Stand with bar at front shoulders, grip just outside shoulders", "Press bar overhead in slight arc", "Lock out arms directly overhead", "Lower with control to shoulders"],
            breathingCue: "Inhale at bottom, exhale pressing up",
            commonMistakes: ["Excessive back arch", "Pressing forward instead of overhead", "Not locking out fully"],
            movementPattern: .verticalPush, tempo: "1-1-2"
        ),
        "Lateral Raise": ExerciseGuide(
            steps: ["Stand with dumbbells at sides, slight bend in elbows", "Raise arms out to sides until parallel to floor", "Pause briefly, lower slowly"],
            breathingCue: "Exhale raising, inhale lowering",
            commonMistakes: ["Using too much weight and swinging", "Raising above shoulder level", "Shrugging shoulders"],
            movementPattern: .lateralRaise, tempo: "2-1-3"
        ),
        "Barbell Curl": ExerciseGuide(
            steps: ["Stand with bar at hip level, shoulder-width underhand grip", "Curl bar up by flexing elbows, keep upper arms still", "Squeeze biceps at top, lower slowly"],
            breathingCue: "Exhale curling up, inhale lowering",
            commonMistakes: ["Swinging body for momentum", "Moving elbows forward", "Not controlling the negative"],
            movementPattern: .curl, tempo: "2-1-3"
        ),
        "Tricep Pushdown": ExerciseGuide(
            steps: ["Face cable machine, grip bar with overhand grip", "Keep upper arms tight to body, push bar down", "Fully extend arms, squeeze triceps", "Return with control"],
            breathingCue: "Exhale pushing down, inhale releasing",
            commonMistakes: ["Elbows flaring out", "Leaning over the bar", "Using momentum"],
            movementPattern: .ext, tempo: "1-1-3"
        ),
        "Barbell Squat": ExerciseGuide(
            steps: ["Bar on upper traps, feet shoulder-width, toes slightly out", "Brace core, break at hips and knees simultaneously", "Descend until thighs are parallel or below", "Drive up through heels, keeping chest up"],
            breathingCue: "Big breath and brace before descending, exhale standing up",
            commonMistakes: ["Knees caving inward", "Rounding lower back", "Not hitting depth", "Weight shifting to toes"],
            movementPattern: .squat, tempo: "2-1-2"
        ),
        "Romanian Deadlift": ExerciseGuide(
            steps: ["Hold bar at hip level, feet hip-width", "Push hips back, lower bar along legs", "Keep slight knee bend, feel stretch in hamstrings", "Drive hips forward to stand, squeeze glutes"],
            breathingCue: "Inhale on the way down, exhale driving hips forward",
            commonMistakes: ["Rounding lower back", "Bending knees too much", "Bar drifting from legs"],
            movementPattern: .hipHinge, tempo: "3-0-2"
        ),
        "Hip Thrust": ExerciseGuide(
            steps: ["Upper back on bench, bar across hips with pad", "Feet flat on floor, shoulder-width", "Drive hips up by squeezing glutes until body is straight", "Pause at top, lower with control"],
            breathingCue: "Exhale thrusting up, inhale lowering",
            commonMistakes: ["Hyperextending lower back at top", "Feet too far from body"],
            movementPattern: .hipHinge, tempo: "1-2-2"
        ),
        "Plank": ExerciseGuide(
            steps: ["Forearms and toes on floor, body in straight line", "Brace core tightly, squeeze glutes", "Hold position without sagging or piking"],
            breathingCue: "Breathe normally while maintaining brace",
            commonMistakes: ["Sagging hips", "Piking hips up", "Holding breath"],
            movementPattern: .staticHold, tempo: "hold"
        ),
        "Farmer's Walk": ExerciseGuide(
            steps: ["Pick up heavy dumbbells/kettlebells at sides", "Stand tall, shoulders back, core braced", "Walk with short controlled steps"],
            breathingCue: "Breathe steadily, keep core braced",
            commonMistakes: ["Leaning to one side", "Shoulders rounding forward"],
            movementPattern: .carry, tempo: "walk"
        ),
    ]
}
