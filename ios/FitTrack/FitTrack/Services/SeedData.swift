import Foundation
import SwiftData

struct SeedData {
    static func seedIfNeeded(context: ModelContext) {
        let descriptor = FetchDescriptor<Exercise>()
        let count = (try? context.fetchCount(descriptor)) ?? 0
        if count > 0 { return }

        let exercises = seedExercises(context: context)
        seedRoutines(context: context, exercises: exercises)
        try? context.save()
    }

    @discardableResult
    static func seedExercises(context: ModelContext) -> [String: Exercise] {
        var map: [String: Exercise] = [:]

        let data: [(String, MuscleGroup, MuscleGroup?)] = [
            // Chest
            ("Barbell Bench Press", .chest, .triceps),
            ("Incline Dumbbell Press", .chest, .shoulders),
            ("Dumbbell Bench Press", .chest, .triceps),
            ("Cable Fly", .chest, nil),
            ("Chest Dip", .chest, .triceps),
            ("Push-Up", .chest, .triceps),
            ("Incline Barbell Press", .chest, .shoulders),
            ("Pec Deck Machine", .chest, nil),
            // Back
            ("Barbell Row", .back, .biceps),
            ("Pull-Up", .back, .biceps),
            ("Lat Pulldown", .back, .biceps),
            ("Seated Cable Row", .back, nil),
            ("Dumbbell Row", .back, .biceps),
            ("T-Bar Row", .back, nil),
            ("Face Pull", .back, .shoulders),
            ("Deadlift", .back, .legs),
            // Shoulders
            ("Overhead Press", .shoulders, .triceps),
            ("Lateral Raise", .shoulders, nil),
            ("Front Raise", .shoulders, nil),
            ("Rear Delt Fly", .shoulders, nil),
            ("Arnold Press", .shoulders, nil),
            ("Dumbbell Shoulder Press", .shoulders, .triceps),
            ("Upright Row", .shoulders, nil),
            // Biceps
            ("Barbell Curl", .biceps, nil),
            ("Dumbbell Curl", .biceps, nil),
            ("Hammer Curl", .biceps, .forearms),
            ("Preacher Curl", .biceps, nil),
            ("Incline Dumbbell Curl", .biceps, nil),
            ("Cable Curl", .biceps, nil),
            ("Concentration Curl", .biceps, nil),
            // Triceps
            ("Tricep Pushdown", .triceps, nil),
            ("Skull Crushers", .triceps, nil),
            ("Overhead Tricep Extension", .triceps, nil),
            ("Close-Grip Bench Press", .triceps, .chest),
            ("Tricep Dip", .triceps, .chest),
            ("Cable Overhead Extension", .triceps, nil),
            // Legs
            ("Barbell Squat", .legs, .glutes),
            ("Leg Press", .legs, .glutes),
            ("Romanian Deadlift", .legs, .glutes),
            ("Leg Extension", .legs, nil),
            ("Leg Curl", .legs, nil),
            ("Bulgarian Split Squat", .legs, .glutes),
            ("Front Squat", .legs, nil),
            ("Hack Squat", .legs, nil),
            ("Walking Lunge", .legs, .glutes),
            // Glutes
            ("Hip Thrust", .glutes, nil),
            ("Glute Bridge", .glutes, nil),
            ("Cable Kickback", .glutes, nil),
            ("Sumo Deadlift", .glutes, .legs),
            // Abs
            ("Plank", .abs, nil),
            ("Cable Crunch", .abs, nil),
            ("Hanging Leg Raise", .abs, nil),
            ("Ab Wheel Rollout", .abs, nil),
            ("Russian Twist", .abs, nil),
            // Calves
            ("Standing Calf Raise", .calves, nil),
            ("Seated Calf Raise", .calves, nil),
            // Forearms
            ("Wrist Curl", .forearms, nil),
            ("Reverse Wrist Curl", .forearms, nil),
            ("Farmer's Walk", .forearms, nil),
        ]

        for (name, group, secondary) in data {
            let exercise = Exercise(name: name, muscleGroup: group, secondaryMuscleGroup: secondary)
            context.insert(exercise)
            map[name] = exercise
        }

        return map
    }

    static func seedRoutines(context: ModelContext, exercises: [String: Exercise]) {
        func ex(_ name: String) -> Exercise { exercises[name]! }

        // 3-Day PPL
        let ppl3 = "PPL 3-Day"
        makeRoutine(context: context, name: "Day 1: Push", desc: "Chest, shoulders, and triceps", targets: "Chest,Shoulders,Triceps", difficulty: "intermediate", days: 3, program: ppl3, order: 1, items: [
            (ex("Barbell Bench Press"), 4, "6-8"), (ex("Incline Dumbbell Press"), 3, "8-12"),
            (ex("Cable Fly"), 3, "12-15"), (ex("Overhead Press"), 3, "6-8"),
            (ex("Lateral Raise"), 3, "12-15"), (ex("Tricep Pushdown"), 3, "10-12")
        ])
        makeRoutine(context: context, name: "Day 2: Pull", desc: "Back and biceps", targets: "Back,Biceps", difficulty: "intermediate", days: 3, program: ppl3, order: 2, items: [
            (ex("Deadlift"), 3, "5-6"), (ex("Barbell Row"), 4, "6-8"),
            (ex("Pull-Up"), 3, "6-10"), (ex("Seated Cable Row"), 3, "10-12"),
            (ex("Face Pull"), 3, "15-20"), (ex("Barbell Curl"), 3, "8-12")
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
            (ex("Barbell Curl"), 2, "10-12"), (ex("Tricep Pushdown"), 2, "10-12")
        ])
        makeRoutine(context: context, name: "Day 2: Full Body B", desc: "Deadlift-focused full body", targets: "Back,Legs,Shoulders,Biceps,Triceps", difficulty: "beginner", days: 3, program: fb3, order: 2, items: [
            (ex("Deadlift"), 3, "5-6"), (ex("Leg Press"), 3, "8-12"),
            (ex("Dumbbell Shoulder Press"), 3, "8-10"), (ex("Lat Pulldown"), 3, "8-12"),
            (ex("Dumbbell Curl"), 3, "10-12"), (ex("Skull Crushers"), 3, "10-12")
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
            (ex("Pec Deck Machine"), 3, "12-15"), (ex("Chest Dip"), 3, "8-12")
        ])
        makeRoutine(context: context, name: "Day 2: Back", desc: "Back-focused session", targets: "Back", difficulty: "intermediate", days: 5, program: bro5, order: 2, items: [
            (ex("Deadlift"), 3, "5-6"), (ex("Barbell Row"), 4, "6-8"),
            (ex("Pull-Up"), 3, "6-10"), (ex("Lat Pulldown"), 3, "8-12"),
            (ex("T-Bar Row"), 3, "8-10"), (ex("Face Pull"), 3, "15-20")
        ])
        makeRoutine(context: context, name: "Day 3: Shoulders", desc: "All three delt heads", targets: "Shoulders", difficulty: "intermediate", days: 5, program: bro5, order: 3, items: [
            (ex("Overhead Press"), 4, "6-8"), (ex("Dumbbell Shoulder Press"), 3, "8-10"),
            (ex("Lateral Raise"), 4, "12-15"), (ex("Front Raise"), 3, "10-12"),
            (ex("Rear Delt Fly"), 4, "12-15"), (ex("Upright Row"), 3, "10-12")
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
            (ex("Cable Fly"), 3, "12-15"), (ex("Face Pull"), 3, "15-20")
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
            (ex("Overhead Tricep Extension"), 3, "12-15")
        ])
        makeRoutine(context: context, name: "Day 4: Pull", desc: "Hypertrophy pull", targets: "Back,Biceps", difficulty: "intermediate", days: 5, program: ulppl5, order: 4, items: [
            (ex("Pull-Up"), 4, "6-10"), (ex("Seated Cable Row"), 3, "10-12"),
            (ex("Dumbbell Row"), 3, "10-12"), (ex("Face Pull"), 4, "15-20"),
            (ex("Dumbbell Curl"), 3, "10-12"), (ex("Hammer Curl"), 3, "10-12"),
            (ex("Concentration Curl"), 3, "10-12")
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
            (ex("Barbell Curl"), 3, "10-12"), (ex("Tricep Pushdown"), 3, "10-12")
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
            (ex("Overhead Tricep Extension"), 3, "12-15")
        ])
        makeRoutine(context: context, name: "Back & Biceps", desc: "Focused back and biceps session", targets: "Back,Biceps", difficulty: "intermediate", items: [
            (ex("Deadlift"), 3, "5-6"), (ex("Pull-Up"), 4, "6-10"),
            (ex("Barbell Row"), 3, "8-10"), (ex("Seated Cable Row"), 3, "10-12"),
            (ex("Face Pull"), 3, "15-20"), (ex("Barbell Curl"), 3, "8-12"),
            (ex("Hammer Curl"), 3, "10-12")
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
