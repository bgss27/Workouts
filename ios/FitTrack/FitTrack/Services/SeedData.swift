import Foundation
import SwiftData

struct SeedData {
    static func seedIfNeeded(context: ModelContext) {
        let descriptor = FetchDescriptor<Exercise>()
        let count = (try? context.fetchCount(descriptor)) ?? 0
        if count > 0 {
            // Existing install: ensure new catalog additions land and equipment
            // tags are correct on legacy rows. Idempotent — safe on every launch.
            retagAndExtendIfNeeded(context: context)
            return
        }

        let exercises = seedExercises(context: context)
        seedRoutines(context: context, exercises: exercises)
        try? context.save()
    }

    /// Source of truth for the built-in exercise catalog. Used by both the
    /// fresh-install seed and the upgrade-path retag/extend step. Mirrors the
    /// Android `SEED_EXERCISES` constant.
    static let catalog: [(String, MuscleGroup, MuscleGroup?, Equipment)] = [
        // Chest — barbells, dumbbells, machines
        ("Barbell Bench Press", .chest, .triceps, .barbell),
        ("Incline Dumbbell Press", .chest, .shoulders, .dumbbell),
        ("Dumbbell Bench Press", .chest, .triceps, .dumbbell),
        ("Cable Fly", .chest, nil, .machine),
        ("Chest Dip", .chest, .triceps, .bodyweight),
        ("Push-Up", .chest, .triceps, .bodyweight),
        ("Incline Barbell Press", .chest, .shoulders, .barbell),
        ("Pec Deck Machine", .chest, nil, .machine),
        // Chest — bodyweight additions for at-home flow
        ("Wide Push-Up", .chest, nil, .bodyweight),
        ("Decline Push-Up", .chest, .triceps, .bodyweight),
        ("Incline Push-Up", .chest, nil, .bodyweight),
        ("Diamond Push-Up", .triceps, .chest, .bodyweight),
        ("Pseudo-Planche Push-Up", .chest, .shoulders, .bodyweight),

        // Back
        ("Barbell Row", .back, .biceps, .barbell),
        ("Pull-Up", .back, .biceps, .pullUpBar),
        ("Lat Pulldown", .back, .biceps, .machine),
        ("Seated Cable Row", .back, nil, .machine),
        ("Dumbbell Row", .back, .biceps, .dumbbell),
        ("T-Bar Row", .back, nil, .barbell),
        ("Face Pull", .back, .shoulders, .machine),
        ("Deadlift", .back, .legs, .barbell),
        // Back — bodyweight additions. Inverted Row tagged pull-up-bar because
        // it needs a low bar / sturdy table to grip — not pure bodyweight.
        ("Inverted Row", .back, .biceps, .pullUpBar),
        ("Superman Hold", .back, nil, .bodyweight),
        ("Reverse Snow Angel", .back, nil, .bodyweight),
        ("Band Pull-Apart", .back, .shoulders, .band),
        // Back — wger-sourced no-equipment additions (wger.de, CC-BY-SA 3.0)
        ("Arm Raises T/Y/I", .back, .shoulders, .bodyweight),
        ("Quadruped Arm and Leg Raise", .back, .glutes, .bodyweight),
        ("Trap-3 Raise", .back, .shoulders, .bodyweight),
        ("Wall Slides", .back, .shoulders, .bodyweight),

        // Shoulders
        ("Overhead Press", .shoulders, .triceps, .barbell),
        ("Lateral Raise", .shoulders, nil, .dumbbell),
        ("Front Raise", .shoulders, nil, .dumbbell),
        ("Rear Delt Fly", .shoulders, nil, .dumbbell),
        ("Arnold Press", .shoulders, nil, .dumbbell),
        ("Dumbbell Shoulder Press", .shoulders, .triceps, .dumbbell),
        ("Upright Row", .shoulders, nil, .barbell),
        // Shoulders — bodyweight
        ("Pike Push-Up", .shoulders, .triceps, .bodyweight),
        ("Decline Pike Push-Up", .shoulders, .triceps, .bodyweight),
        ("Wall Handstand Hold", .shoulders, nil, .bodyweight),

        // Biceps
        ("Barbell Curl", .biceps, nil, .barbell),
        ("Dumbbell Curl", .biceps, nil, .dumbbell),
        ("Hammer Curl", .biceps, .forearms, .dumbbell),
        ("Preacher Curl", .biceps, nil, .dumbbell),
        ("Incline Dumbbell Curl", .biceps, nil, .dumbbell),
        ("Cable Curl", .biceps, nil, .machine),
        ("Concentration Curl", .biceps, nil, .dumbbell),
        ("Band Curl", .biceps, nil, .band),
        ("Chin-Up", .biceps, .back, .pullUpBar),
        // Biceps — true no-equipment options
        ("Isometric Biceps Flex", .biceps, nil, .bodyweight),
        ("Self-Resistance Curl", .biceps, nil, .bodyweight),
        ("Towel Biceps Curl", .biceps, .forearms, .bodyweight),
        ("Side Biceps Hold", .biceps, .shoulders, .bodyweight),

        // Triceps
        ("Tricep Pushdown", .triceps, nil, .machine),
        ("Skull Crushers", .triceps, nil, .barbell),
        ("Overhead Tricep Extension", .triceps, nil, .dumbbell),
        ("Close-Grip Bench Press", .triceps, .chest, .barbell),
        ("Tricep Dip", .triceps, .chest, .bodyweight),
        ("Cable Overhead Extension", .triceps, nil, .machine),
        ("Chair Dip", .triceps, .chest, .bodyweight),

        // Legs
        ("Barbell Squat", .legs, .glutes, .barbell),
        ("Leg Press", .legs, .glutes, .machine),
        ("Romanian Deadlift", .legs, .glutes, .barbell),
        ("Leg Extension", .legs, nil, .machine),
        ("Leg Curl", .legs, nil, .machine),
        ("Bulgarian Split Squat", .legs, .glutes, .bodyweight),
        ("Front Squat", .legs, nil, .barbell),
        ("Hack Squat", .legs, nil, .machine),
        ("Walking Lunge", .legs, .glutes, .bodyweight),
        // Legs — bodyweight
        ("Bodyweight Squat", .legs, .glutes, .bodyweight),
        ("Jump Squat", .legs, .glutes, .bodyweight),
        ("Pistol Squat", .legs, .glutes, .bodyweight),
        ("Wall Sit", .legs, nil, .bodyweight),
        ("Reverse Lunge", .legs, .glutes, .bodyweight),

        // Glutes
        ("Hip Thrust", .glutes, nil, .barbell),
        ("Glute Bridge", .glutes, nil, .bodyweight),
        ("Cable Kickback", .glutes, nil, .machine),
        ("Sumo Deadlift", .glutes, .legs, .barbell),
        ("Single-Leg Glute Bridge", .glutes, nil, .bodyweight),
        ("Fire Hydrant", .glutes, nil, .bodyweight),

        // Abs
        ("Plank", .abs, nil, .bodyweight),
        ("Cable Crunch", .abs, nil, .machine),
        ("Hanging Leg Raise", .abs, nil, .pullUpBar),
        ("Ab Wheel Rollout", .abs, nil, .bodyweight),
        ("Russian Twist", .abs, nil, .bodyweight),
        ("Bicycle Crunch", .abs, nil, .bodyweight),
        ("Mountain Climbers", .abs, nil, .bodyweight),
        ("Dead Bug", .abs, nil, .bodyweight),
        ("Hollow Hold", .abs, nil, .bodyweight),
        ("Lying Leg Raise", .abs, nil, .bodyweight),

        // Calves
        ("Standing Calf Raise", .calves, nil, .machine),
        ("Seated Calf Raise", .calves, nil, .machine),
        ("Bodyweight Calf Raise", .calves, nil, .bodyweight),
        ("Single-Leg Calf Raise", .calves, nil, .bodyweight),

        // Forearms / grip
        ("Wrist Curl", .forearms, nil, .dumbbell),
        ("Reverse Wrist Curl", .forearms, nil, .dumbbell),
        ("Farmer's Walk", .forearms, nil, .dumbbell),
        ("Dead Hang", .forearms, nil, .pullUpBar),
    ]

    @discardableResult
    static func seedExercises(context: ModelContext) -> [String: Exercise] {
        var map: [String: Exercise] = [:]
        for (name, group, secondary, equipment) in catalog {
            let exercise = Exercise(
                name: name,
                muscleGroup: group,
                secondaryMuscleGroup: secondary,
                equipment: equipment
            )
            context.insert(exercise)
            map[name] = exercise
        }
        return map
    }

    /// Upgrade path for existing installs from before v6 (the equipment tag) /
    /// v7 (wger additions): retag built-in exercises to their correct
    /// equipment and insert any new catalog entries the user doesn't have yet.
    /// Custom user-created exercises are untouched.
    ///
    /// Defensive against duplicate names — `Dictionary(uniqueKeysWithValues:)`
    /// fatal-errors on duplicates, which would crash the app on launch if the
    /// user happens to have e.g. a custom "Push-Up" alongside the built-in
    /// one. We use a regular dictionary populated last-write-wins instead.
    static func retagAndExtendIfNeeded(context: ModelContext) {
        let existing = (try? context.fetch(FetchDescriptor<Exercise>())) ?? []
        // Group by name to tolerate any duplicates that snuck in.
        var byName: [String: [Exercise]] = [:]
        for row in existing { byName[row.name, default: []].append(row) }
        var changed = false
        for (name, group, secondary, equipment) in catalog {
            if let rows = byName[name], !rows.isEmpty {
                // Retag every built-in row matching this name; leave custom
                // user-created rows alone.
                for row in rows where !row.isCustom {
                    if row.equipment != equipment {
                        row.equipment = equipment
                        changed = true
                    }
                }
            } else {
                context.insert(Exercise(
                    name: name,
                    muscleGroup: group,
                    secondaryMuscleGroup: secondary,
                    equipment: equipment
                ))
                changed = true
            }
        }
        if changed { try? context.save() }
    }

    static func seedRoutines(context: ModelContext, exercises: [String: Exercise]) {
        func ex(_ name: String) -> Exercise { exercises[name]! }

        // 3-Day PPL
        let ppl3 = "PPL 3-Day"
        makeRoutine(context: context, name: "Day 1: Push", desc: "Chest, shoulders, and triceps", targets: "Chest,Shoulders,Triceps", difficulty: "intermediate", days: 3, program: ppl3, order: 1, items: [
            (ex("Barbell Bench Press"), 4, "6-8"), (ex("Incline Dumbbell Press"), 3, "8-12"),
            (ex("Cable Fly"), 3, "12-15"), (ex("Overhead Press"), 3, "6-8"),
            (ex("Lateral Raise"), 3, "12-15"), (ex("Tricep Pushdown"), 3, "10-12"),
            (ex("Hanging Leg Raise"), 3, "8-12")
        ])
        makeRoutine(context: context, name: "Day 2: Pull", desc: "Back and biceps", targets: "Back,Biceps", difficulty: "intermediate", days: 3, program: ppl3, order: 2, items: [
            (ex("Deadlift"), 3, "5-6"), (ex("Barbell Row"), 4, "6-8"),
            (ex("Pull-Up"), 3, "6-10"), (ex("Seated Cable Row"), 3, "10-12"),
            (ex("Face Pull"), 3, "15-20"), (ex("Barbell Curl"), 3, "8-12"),
            (ex("Cable Crunch"), 3, "12-15")
        ])
        makeRoutine(context: context, name: "Day 3: Legs", desc: "Quads, hamstrings, glutes, and calves", targets: "Legs,Glutes,Calves", difficulty: "intermediate", days: 3, program: ppl3, order: 3, items: [
            (ex("Barbell Squat"), 4, "5-8"), (ex("Leg Press"), 3, "8-12"),
            (ex("Romanian Deadlift"), 3, "8-12"), (ex("Leg Extension"), 3, "12-15"),
            (ex("Leg Curl"), 3, "10-12"), (ex("Hip Thrust"), 3, "8-12"),
            (ex("Standing Calf Raise"), 4, "12-15")
        ])

        // 3-Day Full Body
        let fb3 = "Full Body 3-Day"
        makeRoutine(context: context, name: "Day 1: Full Body A", desc: "Squat-focused full body", targets: "Legs,Chest,Back,Shoulders", difficulty: "beginner", days: 3, program: fb3, order: 1, items: [
            (ex("Barbell Squat"), 3, "5-8"), (ex("Barbell Bench Press"), 3, "6-8"),
            (ex("Barbell Row"), 3, "8-10"), (ex("Overhead Press"), 3, "8-10"),
            (ex("Barbell Curl"), 2, "10-12"), (ex("Tricep Pushdown"), 2, "10-12"),
            (ex("Plank"), 3, "30-60")
        ])
        makeRoutine(context: context, name: "Day 2: Full Body B", desc: "Deadlift-focused full body", targets: "Back,Legs,Shoulders,Biceps,Triceps", difficulty: "beginner", days: 3, program: fb3, order: 2, items: [
            (ex("Deadlift"), 3, "5-6"), (ex("Leg Press"), 3, "8-12"),
            (ex("Dumbbell Shoulder Press"), 3, "8-10"), (ex("Lat Pulldown"), 3, "8-12"),
            (ex("Dumbbell Curl"), 3, "10-12"), (ex("Skull Crushers"), 3, "10-12"),
            (ex("Cable Crunch"), 3, "12-15")
        ])
        makeRoutine(context: context, name: "Day 3: Full Body C", desc: "Front squat-focused full body", targets: "Legs,Chest,Back,Abs", difficulty: "beginner", days: 3, program: fb3, order: 3, items: [
            (ex("Front Squat"), 3, "6-8"), (ex("Dumbbell Bench Press"), 3, "8-12"),
            (ex("Pull-Up"), 3, "6-10"), (ex("Lateral Raise"), 3, "12-15"),
            (ex("Hip Thrust"), 3, "8-12"), (ex("Hanging Leg Raise"), 3, "10-15")
        ])

        // 5-Day Bro Split
        let bro5 = "Bro Split 5-Day"
        makeRoutine(context: context, name: "Day 1: Chest", desc: "Chest-focused session", targets: "Chest", difficulty: "intermediate", days: 5, program: bro5, order: 1, items: [
            (ex("Barbell Bench Press"), 4, "6-8"), (ex("Incline Dumbbell Press"), 3, "8-12"),
            (ex("Incline Barbell Press"), 3, "8-10"), (ex("Cable Fly"), 3, "12-15"),
            (ex("Pec Deck Machine"), 3, "12-15"), (ex("Chest Dip"), 3, "8-12"),
            (ex("Plank"), 3, "30-60")
        ])
        makeRoutine(context: context, name: "Day 2: Back", desc: "Back-focused session", targets: "Back", difficulty: "intermediate", days: 5, program: bro5, order: 2, items: [
            (ex("Deadlift"), 3, "5-6"), (ex("Barbell Row"), 4, "6-8"),
            (ex("Pull-Up"), 3, "6-10"), (ex("Lat Pulldown"), 3, "8-12"),
            (ex("T-Bar Row"), 3, "8-10"), (ex("Face Pull"), 3, "15-20"),
            (ex("Hanging Leg Raise"), 3, "8-12")
        ])
        makeRoutine(context: context, name: "Day 3: Shoulders", desc: "All three delt heads", targets: "Shoulders", difficulty: "intermediate", days: 5, program: bro5, order: 3, items: [
            (ex("Overhead Press"), 4, "6-8"), (ex("Dumbbell Shoulder Press"), 3, "8-10"),
            (ex("Lateral Raise"), 4, "12-15"), (ex("Front Raise"), 3, "10-12"),
            (ex("Rear Delt Fly"), 4, "12-15"), (ex("Upright Row"), 3, "10-12"),
            (ex("Cable Crunch"), 3, "12-15")
        ])
        makeRoutine(context: context, name: "Day 4: Legs", desc: "Full leg day", targets: "Legs,Glutes,Calves", difficulty: "intermediate", days: 5, program: bro5, order: 4, items: [
            (ex("Barbell Squat"), 4, "5-8"), (ex("Leg Press"), 3, "8-12"),
            (ex("Romanian Deadlift"), 3, "8-12"), (ex("Leg Extension"), 3, "12-15"),
            (ex("Leg Curl"), 3, "10-12"), (ex("Hip Thrust"), 3, "8-12"),
            (ex("Standing Calf Raise"), 4, "12-15")
        ])
        makeRoutine(context: context, name: "Day 5: Arms", desc: "Biceps, triceps, and forearms", targets: "Biceps,Triceps,Forearms", difficulty: "intermediate", days: 5, program: bro5, order: 5, items: [
            (ex("Barbell Curl"), 3, "8-10"), (ex("Skull Crushers"), 3, "8-10"),
            (ex("Hammer Curl"), 3, "10-12"), (ex("Tricep Pushdown"), 3, "10-12"),
            (ex("Incline Dumbbell Curl"), 3, "10-12"), (ex("Overhead Tricep Extension"), 3, "12-15"),
            (ex("Concentration Curl"), 3, "10-12"), (ex("Wrist Curl"), 3, "12-15")
        ])

        // 5-Day Upper/Lower/PPL
        let ulppl5 = "Upper/Lower/PPL 5-Day"
        makeRoutine(context: context, name: "Day 1: Upper Body", desc: "Heavy upper compounds", targets: "Chest,Back,Shoulders", difficulty: "intermediate", days: 5, program: ulppl5, order: 1, items: [
            (ex("Barbell Bench Press"), 4, "5-6"), (ex("Barbell Row"), 4, "6-8"),
            (ex("Overhead Press"), 3, "6-8"), (ex("Lat Pulldown"), 3, "8-12"),
            (ex("Cable Fly"), 3, "12-15"), (ex("Face Pull"), 3, "15-20"),
            (ex("Plank"), 3, "30-60")
        ])
        makeRoutine(context: context, name: "Day 2: Lower Body", desc: "Heavy lower compounds", targets: "Legs,Glutes,Calves", difficulty: "intermediate", days: 5, program: ulppl5, order: 2, items: [
            (ex("Barbell Squat"), 4, "5-6"), (ex("Romanian Deadlift"), 3, "8-12"),
            (ex("Leg Press"), 3, "8-12"), (ex("Hip Thrust"), 3, "8-12"),
            (ex("Bulgarian Split Squat"), 3, "8-12"), (ex("Standing Calf Raise"), 4, "12-15")
        ])
        makeRoutine(context: context, name: "Day 3: Push", desc: "Hypertrophy push", targets: "Chest,Shoulders,Triceps", difficulty: "intermediate", days: 5, program: ulppl5, order: 3, items: [
            (ex("Incline Dumbbell Press"), 4, "8-12"), (ex("Dumbbell Bench Press"), 3, "10-12"),
            (ex("Cable Fly"), 3, "12-15"), (ex("Dumbbell Shoulder Press"), 3, "10-12"),
            (ex("Lateral Raise"), 4, "12-15"), (ex("Tricep Pushdown"), 3, "10-12"),
            (ex("Overhead Tricep Extension"), 3, "12-15"),
            (ex("Hanging Leg Raise"), 3, "8-12")
        ])
        makeRoutine(context: context, name: "Day 4: Pull", desc: "Hypertrophy pull", targets: "Back,Biceps", difficulty: "intermediate", days: 5, program: ulppl5, order: 4, items: [
            (ex("Pull-Up"), 4, "6-10"), (ex("Seated Cable Row"), 3, "10-12"),
            (ex("Dumbbell Row"), 3, "10-12"), (ex("Face Pull"), 4, "15-20"),
            (ex("Dumbbell Curl"), 3, "10-12"), (ex("Hammer Curl"), 3, "10-12"),
            (ex("Concentration Curl"), 3, "10-12"),
            (ex("Cable Crunch"), 3, "12-15")
        ])
        makeRoutine(context: context, name: "Day 5: Legs", desc: "Hypertrophy legs", targets: "Legs,Glutes,Calves", difficulty: "intermediate", days: 5, program: ulppl5, order: 5, items: [
            (ex("Front Squat"), 4, "8-10"), (ex("Hack Squat"), 3, "8-12"),
            (ex("Leg Extension"), 3, "12-15"), (ex("Leg Curl"), 3, "10-12"),
            (ex("Walking Lunge"), 3, "10-12"), (ex("Standing Calf Raise"), 4, "12-15"),
            (ex("Seated Calf Raise"), 4, "15-20")
        ])

        // Standalone routines
        makeRoutine(context: context, name: "Upper Body", desc: "Complete upper body workout", targets: "Chest,Back,Shoulders,Biceps,Triceps", difficulty: "intermediate", items: [
            (ex("Barbell Bench Press"), 4, "6-8"), (ex("Barbell Row"), 4, "6-8"),
            (ex("Overhead Press"), 3, "8-10"), (ex("Lat Pulldown"), 3, "8-12"),
            (ex("Barbell Curl"), 3, "10-12"), (ex("Tricep Pushdown"), 3, "10-12"),
            (ex("Plank"), 3, "30-60")
        ])
        makeRoutine(context: context, name: "Lower Body", desc: "Complete lower body workout", targets: "Legs,Glutes,Calves", difficulty: "intermediate", items: [
            (ex("Barbell Squat"), 4, "5-8"), (ex("Romanian Deadlift"), 3, "8-12"),
            (ex("Leg Press"), 3, "8-12"), (ex("Bulgarian Split Squat"), 3, "8-12"),
            (ex("Hip Thrust"), 3, "8-12"), (ex("Standing Calf Raise"), 4, "12-15"),
            (ex("Seated Calf Raise"), 4, "15-20")
        ])
        makeRoutine(context: context, name: "Chest & Triceps", desc: "Focused chest and triceps session", targets: "Chest,Triceps", difficulty: "intermediate", items: [
            (ex("Barbell Bench Press"), 4, "6-8"), (ex("Incline Dumbbell Press"), 3, "8-12"),
            (ex("Dumbbell Bench Press"), 3, "8-12"), (ex("Cable Fly"), 3, "12-15"),
            (ex("Skull Crushers"), 3, "10-12"), (ex("Tricep Pushdown"), 3, "10-12"),
            (ex("Overhead Tricep Extension"), 3, "12-15"),
            (ex("Hanging Leg Raise"), 3, "8-12")
        ])
        makeRoutine(context: context, name: "Back & Biceps", desc: "Focused back and biceps session", targets: "Back,Biceps", difficulty: "intermediate", items: [
            (ex("Deadlift"), 3, "5-6"), (ex("Pull-Up"), 4, "6-10"),
            (ex("Barbell Row"), 3, "8-10"), (ex("Seated Cable Row"), 3, "10-12"),
            (ex("Face Pull"), 3, "15-20"), (ex("Barbell Curl"), 3, "8-12"),
            (ex("Hammer Curl"), 3, "10-12"),
            (ex("Cable Crunch"), 3, "12-15")
        ])
    }

    private static func makeRoutine(
        context: ModelContext,
        name: String, desc: String, targets: String, difficulty: String = "intermediate",
        days: Int = 0, program: String? = nil, order: Int = 0,
        items: [(Exercise, Int, String)]
    ) {
        let routine = Routine(
            name: name, description: desc, targetMuscleGroups: targets,
            difficulty: difficulty, daysPerWeek: days, programName: program, dayOrder: order
        )
        context.insert(routine)
        for (index, item) in items.enumerated() {
            let re = RoutineExercise(exercise: item.0, orderIndex: index, suggestedSets: item.1, suggestedReps: item.2)
            re.routine = routine
            routine.exercises.append(re)
        }
    }
}
