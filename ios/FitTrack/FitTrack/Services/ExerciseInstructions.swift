import Foundation

/// Hand-written "how to do this" instructions for catalog entries the
/// open-source `OpenExerciseDb` dataset doesn't cover — the four no-equipment
/// biceps moves, the wger-sourced back work, and novel bodyweight variants.
///
/// Mirrors the Android `ExerciseInstructions` lookup. Updates ship via app
/// release; no DB migration needed.
enum ExerciseInstructions {
    private static let map: [String: String] = [
        // Bodyweight chest variants the open DB doesn't cover well
        "Diamond Push-Up": "Hands together under your chest forming a diamond shape with thumbs and index fingers. Elbows stay close to body. Hits triceps hardest; secondarily chest.",
        "Pseudo-Planche Push-Up": "Standard push-up but hands placed by your hips (fingers pointing back or out), shoulders shifted forward over hands. Lean forward as you lower. Very challenging — start with short ranges.",

        // Back — limited bodyweight options
        "Inverted Row": "Lie under a sturdy waist-high table or low bar. Grip the edge palms-up, body straight with heels on the floor. Pull your chest up to the bar, squeeze shoulder blades, lower with control. 3-4 sets of 8-15 reps.",
        "Superman Hold": "Lie face down, arms extended forward. Lift arms, chest, and legs off the floor simultaneously, squeezing your lower back and glutes. Hold 10-30 seconds.",
        "Reverse Snow Angel": "Lie face down with arms by your sides, palms down. Lift arms slightly off the floor and sweep them in an arc out to overhead and back, keeping arms straight. Works rear delts and upper-back.",
        "Band Pull-Apart": "Hold a resistance band at shoulder height with arms extended, hands shoulder-width apart. Pull the band apart by squeezing your shoulder blades. Slow tempo, 15-20 reps. Great for posture.",
        // wger-sourced no-equipment back exercises (wger.de, CC-BY-SA 3.0)
        "Arm Raises T/Y/I": "Stand upright, feet hip-width, core engaged. Raise both arms in three patterns: out to the sides in a T, then up and out at 45° forming a Y, then straight overhead forming an I. 8-10 reps of each shape. (via wger)",
        "Quadruped Arm and Leg Raise": "Get on all fours, hands under shoulders, knees under hips. Extend your right arm forward and your left leg back simultaneously, body in a straight line. Hold a beat, switch sides. (via wger)",
        "Trap-3 Raise": "Hinge forward at the hips, knees slightly bent, arms hanging down. Depress and retract your shoulder blade as you raise one arm up and slightly forward at a 45° angle. (via wger)",
        "Wall Slides": "Stand with heels, shoulders, back of head, and hips touching a wall. Start with elbows bent 90° and forearms touching the wall. Slide your arms up overhead while keeping every contact point against the wall, then back down. (via wger)",

        // Shoulders — bodyweight
        "Pike Push-Up": "Start in a downward-dog position: hips high, body forming an inverted V. Bend elbows to lower the top of your head toward the floor between your hands, then press back up. Targets shoulders rather than chest.",
        "Decline Pike Push-Up": "Pike push-up with feet elevated on a couch or chair, so your body is closer to vertical. Much harder than floor pike push-up.",
        "Wall Handstand Hold": "Walk your feet up a wall until you're inverted, hands on the floor about a foot from the wall. Hold 15-45 seconds with shoulders pressed up. Builds shoulder stability and strength.",

        // Biceps — pure no-equipment options (Android-sourced)
        "Band Curl": "Stand on a resistance band with both feet, holding the band by your sides palms up. Curl your hands toward your shoulders, keeping elbows pinned.",
        "Isometric Biceps Flex": "Stand or sit. Bend your elbow 90° and flex your biceps as hard as you can — squeeze for 10-15 seconds, rest, repeat. Do 3-4 sets per arm.",
        "Self-Resistance Curl": "Cup your right hand under your left palm. As you curl your left arm up, push DOWN with the right hand to resist. Then reverse roles. Slow tempo, 10-12 reps each side.",
        "Towel Biceps Curl": "Hold the ends of a sturdy towel in each hand with elbows bent 90° at your sides. Pull outward on the towel (it won't move) while curling — your own pulling force becomes the resistance.",
        "Side Biceps Hold": "Stand with arms straight out to the sides at shoulder height, palms forward. Flex biceps hard — bend elbows to bring fists toward shoulders without letting upper arms drop. Hold 5s, extend slowly.",

        // Triceps — bodyweight
        "Chair Dip": "Sit on the edge of a sturdy chair, hands gripping the edge by your hips. Slide your butt off and lower your body by bending elbows to 90°, then press back up. Feet forward = harder, bent = easier.",

        // Legs — bodyweight
        "Jump Squat": "Bodyweight squat into an explosive jump at the top. Land softly into the next rep. Plyometric — keep sets short (6-10 reps) and focus on quality of jumps.",
        "Pistol Squat": "Single-leg squat: one leg extended straight in front of you, hold arms out for balance. Lower to a deep squat on one leg, then stand. Advanced — start with hand support or partial range.",
        "Wall Sit": "Back flat against a wall, slide down until knees are at 90°. Hold 30-60 seconds. Quad-burning isometric.",
        "Reverse Lunge": "Step one foot back into a lunge, lowering until front knee is 90° and back knee hovers above the floor. Drive through front heel to return. Alternate legs.",

        // Glutes
        "Fire Hydrant": "On all fours, hips and shoulders square. Lift one knee out to the side keeping the bend at 90°, like a dog at a hydrant. Squeeze glutes at the top. 12-15 each side.",

        // Abs — bodyweight
        "Bicycle Crunch": "Lie on your back, hands behind head, knees lifted. Bring opposite elbow to opposite knee while extending the other leg. Alternate in a pedaling motion.",
        "Hollow Hold": "Lie on your back, lift legs and shoulders off the floor, arms extended overhead by your ears. Body forms a shallow banana shape, lower back pressed into the floor. Hold 15-30 seconds.",

        // Calves
        "Bodyweight Calf Raise": "Stand with feet hip-width. Push up onto the balls of your feet, pause at the top, lower with control. For more range, stand with toes on a step and let heels drop below.",
        "Single-Leg Calf Raise": "Same as bodyweight calf raise but one leg at a time. Use a wall for balance.",

        // Forearms / grip
        "Dead Hang": "Hang from a pull-up bar with straight arms, feet off the floor. Hold 30-60 seconds. Builds grip endurance and decompresses the spine.",
    ]

    static func get(_ exerciseName: String) -> String? { map[exerciseName] }
}

/// Single entry point used by views — open-DB first, hand-written fallback.
enum ExerciseDescriptions {
    static func text(for exerciseName: String) -> String? {
        OpenExerciseDb.instructions(for: exerciseName)
            ?? ExerciseInstructions.get(exerciseName)
    }
}
