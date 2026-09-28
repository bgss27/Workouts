package com.fittrack.app.data.repository

/**
 * Plain-text "how to do this" instructions for the catalog. Especially valuable
 * for the bodyweight / band / pull-up-bar exercises added in v5 — many users
 * won't recognize names like "Inverted Row" or "Pseudo-Planche Push-Up" and
 * the existing image gallery doesn't cover them.
 *
 * Kept as a static map (not a DB column) so updates ship via app update
 * without a migration. Keys MUST match the [Exercise.name] in the seed list.
 */
object ExerciseInstructions {
    private val map: Map<String, String> = mapOf(
        // Chest — bodyweight push-up variations
        "Push-Up" to "Hands shoulder-width on the floor, body straight from head to heels. Lower until chest is just above the floor, then press back up. Keep elbows ~45° from your sides.",
        "Wide Push-Up" to "Push-up with hands placed wider than shoulders (1.5x shoulder width). Emphasizes outer chest. Keep elbows tracking down, not flaring at 90°.",
        "Decline Push-Up" to "Feet elevated on a couch, bed, or chair (12-18\"). Hands on the floor in standard push-up position. Harder than regular push-ups and targets upper chest more.",
        "Incline Push-Up" to "Hands elevated on a counter, table, or sturdy chair. Easier than floor push-ups — useful for high-rep work or beginners. The lower the surface, the harder.",
        "Diamond Push-Up" to "Hands together under your chest forming a diamond shape with thumbs and index fingers. Elbows stay close to body. Hits triceps hardest; secondarily chest.",
        "Pseudo-Planche Push-Up" to "Standard push-up but hands placed by your hips (fingers pointing back or out), shoulders shifted forward over hands. Lean forward as you lower. Very challenging — start with short ranges.",

        // Back — limited bodyweight options
        "Inverted Row" to "Lie under a sturdy waist-high table or low bar. Grip the edge palms-up, body straight with heels on the floor. Pull your chest up to the bar, squeeze shoulder blades, lower with control. 3-4 sets of 8-15 reps.",
        "Superman Hold" to "Lie face down, arms extended forward. Lift arms, chest, and legs off the floor simultaneously, squeezing your lower back and glutes. Hold 10-30 seconds. Builds posterior chain isometric strength.",
        "Reverse Snow Angel" to "Lie face down with arms by your sides, palms down. Lift arms slightly off the floor and sweep them in an arc out to overhead and back, keeping arms straight. Works rear delts and upper-back.",
        "Band Pull-Apart" to "Hold a resistance band at shoulder height with arms extended, hands shoulder-width apart. Pull the band apart by squeezing your shoulder blades. Slow tempo, 15-20 reps. Great for posture.",

        // v7 wger-sourced no-equipment back exercises (wger.de, CC-BY-SA 3.0).
        "Arm Raises T/Y/I" to "Stand upright, feet hip-width, core engaged. Raise both arms in three patterns: out to the sides in a T, then up and out at 45° forming a Y, then straight overhead forming an I. 8-10 reps of each shape. Hits rear delts, traps, and upper back. (via wger)",
        "Quadruped Arm and Leg Raise" to "Get on all fours, hands under shoulders, knees under hips. Extend your right arm forward and your left leg back simultaneously, body in a straight line. Hold a beat, switch sides. Slow and controlled — 10-12 each side. (via wger)",
        "Trap-3 Raise" to "Hinge forward at the hips, knees slightly bent, arms hanging down. Depress and retract your shoulder blade as you raise one arm up and slightly forward at a 45° angle (like the diagonal of the trapezius). Slow, 10-12 each side. (via wger)",
        "Wall Slides" to "Stand with heels, shoulders, back of head, and hips touching a wall. Start with elbows bent 90° and forearms touching the wall. Slide your arms up overhead while keeping every contact point against the wall, then back down. Brutal for upper-back posture muscles. (via wger)",

        // Shoulders — bodyweight
        "Pike Push-Up" to "Start in a downward-dog position: hips high, body forming an inverted V. Bend elbows to lower the top of your head toward the floor between your hands, then press back up. Targets shoulders rather than chest.",
        "Decline Pike Push-Up" to "Pike push-up with feet elevated on a couch or chair, so your body is closer to vertical. Much harder than floor pike push-up. Use a small range first if needed.",
        "Wall Handstand Hold" to "Walk your feet up a wall until you're inverted, hands on the floor about a foot from the wall. Hold 15-45 seconds with shoulders pressed up (no shrugging). Builds shoulder stability and strength.",

        // Biceps — limited bodyweight; pull-up bar / band are the real options
        "Chin-Up" to "Hang from a pull-up bar with palms facing you, hands shoulder-width. Pull until your chin clears the bar, lower with control. Biceps-dominant version of the pull-up. 3-4 sets of 5-10.",
        "Band Curl" to "Stand on a resistance band with both feet, holding the band by your sides palms up. Curl your hands toward your shoulders, keeping elbows pinned. Resistance builds as you curl — slow at the top.",
        // Biceps — pure no-equipment options
        "Isometric Biceps Flex" to "Stand or sit. Bend your elbow 90° and flex your biceps as hard as you can — squeeze for 10-15 seconds, rest, repeat. Do 3-4 sets per arm. Pure isometric — builds strength even without external load.",
        "Self-Resistance Curl" to "Cup your right hand under your left palm. As you curl your left arm up, push DOWN with the right hand to resist. Then reverse roles. Slow tempo, 10-12 reps each side. Manual resistance from your other arm.",
        "Towel Biceps Curl" to "Hold the ends of a sturdy towel in each hand with elbows bent 90° at your sides. Pull outward on the towel (it won't move) while curling — your own pulling force becomes the resistance. 3 sets of 10-15.",
        "Side Biceps Hold" to "Stand with arms straight out to the sides at shoulder height, palms forward. Flex biceps hard — bend elbows to bring fists toward shoulders without letting upper arms drop. Hold the flex 5 seconds, extend slowly. 12-15 reps.",

        // Triceps — bodyweight
        "Chair Dip" to "Sit on the edge of a sturdy chair, hands gripping the edge by your hips. Slide your butt off and lower your body by bending elbows to 90°, then press back up. Feet forward = harder, bent = easier.",

        // Legs — bodyweight
        "Bodyweight Squat" to "Feet shoulder-width, toes slightly out. Sit back into your heels, lowering thighs at least parallel to the floor. Knees track over toes. Drive through heels to stand. 3 sets of 15-25.",
        "Jump Squat" to "Bodyweight squat into an explosive jump at the top. Land softly into the next rep. Plyometric — keep sets short (6-10 reps) and focus on quality of jumps.",
        "Pistol Squat" to "Single-leg squat: one leg extended straight in front of you, hold arms out for balance. Lower to a deep squat on one leg, then stand. Advanced — start with hand support or partial range.",
        "Wall Sit" to "Back flat against a wall, slide down until knees are at 90° (thighs parallel to floor). Hold 30-60 seconds. Quad-burning isometric — keep weight in your heels.",
        "Reverse Lunge" to "Step one foot back into a lunge, lowering until front knee is 90° and back knee hovers above the floor. Drive through front heel to return. Alternate legs. Easier on knees than forward lunges.",

        // Glutes — bodyweight
        "Glute Bridge" to "Lie on your back, knees bent, feet flat on the floor close to your butt. Drive through heels to lift your hips until your body forms a straight line from shoulders to knees. Squeeze glutes at the top.",
        "Single-Leg Glute Bridge" to "Glute bridge but with one foot off the floor, leg extended or knee tucked. Same hip-drive motion, single leg loaded. Stronger glute focus and core stability.",
        "Fire Hydrant" to "On all fours, hips and shoulders square. Lift one knee out to the side keeping the bend at 90°, like a dog at a hydrant. Squeeze glutes at the top. 12-15 each side.",

        // Abs — bodyweight
        "Plank" to "Forearms on the floor, body straight from head to heels, core braced. Hold 30-60 seconds. Don't let hips sag or pike up — squeeze glutes and quads to stay aligned.",
        "Bicycle Crunch" to "Lie on your back, hands behind head, knees lifted. Bring opposite elbow to opposite knee while extending the other leg. Alternate in a pedaling motion. Slow and controlled, not racing.",
        "Mountain Climbers" to "From a plank position, drive one knee toward your chest, then switch quickly — like running in place horizontally. 30-60 seconds. Keeps the core braced while raising the heart rate.",
        "Dead Bug" to "Lie on your back, arms straight up, knees bent 90° (tabletop). Lower opposite arm and leg toward the floor without arching your back, return, switch. Slow tempo — quality over speed.",
        "Hollow Hold" to "Lie on your back, lift legs and shoulders off the floor, arms extended overhead by your ears. Body forms a shallow banana shape, lower back pressed into the floor. Hold 15-30 seconds.",
        "Lying Leg Raise" to "Lie on your back, legs straight, hands under your hips for support. Raise legs to vertical (or as high as you can without arching), lower slowly without touching the floor. 10-15 reps.",
        "Russian Twist" to "Sit on the floor, knees bent, feet hovering or flat. Lean back ~45°, rotate your torso side to side, touching the floor beside each hip. Add a weight or water bottle for resistance.",
        "Ab Wheel Rollout" to "Kneel holding an ab wheel (or barbell with plates). Roll forward extending your body, then pull back using your abs. Don't let your hips sag. Advanced — start short and progress.",

        // Calves — bodyweight
        "Bodyweight Calf Raise" to "Stand with feet hip-width. Push up onto the balls of your feet, pause at the top, lower with control. For more range, stand with toes on a step and let heels drop below. 15-25 reps.",
        "Single-Leg Calf Raise" to "Same as bodyweight calf raise but one leg at a time. Use a wall for balance. Stronger stimulus than the two-leg version.",

        // Forearms / grip
        "Dead Hang" to "Hang from a pull-up bar with straight arms, feet off the floor. Hold as long as you can (target 30-60 seconds). Builds grip endurance and decompresses the spine.",

        // Existing exercises that are common at home — adding for completeness
        "Bulgarian Split Squat" to "Stand 2-3 feet in front of a bench, rear foot resting on the bench laces-down. Lower your front thigh to parallel, then drive through the heel to stand. 3 sets of 8-12 each leg.",
        "Walking Lunge" to "Step forward into a lunge, lowering until both knees are ~90°. Push off the front foot to bring the back leg forward into the next lunge. Keep torso upright.",
    )

    fun get(exerciseName: String): String? = map[exerciseName]
}
