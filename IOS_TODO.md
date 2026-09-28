# iOS — Deferred Features

Tracks features shipped on Android but not yet on iOS. Work through this list when prioritising iOS catch-up work.

Last updated: 2026-05-20 (post first iOS catch-up batch)

## Xcode project file

The new files and the bundled JSON were patched into `project.pbxproj` directly — no manual Xcode "Add Files…" step needed. Verified clean with `xcodebuild -scheme FitTrack -sdk iphonesimulator build` → **BUILD SUCCEEDED**.

A backup of the pre-patch project file lives at `FitTrack.xcodeproj/project.pbxproj.bak.pre-iosbatch`. Safe to delete once you've confirmed the project still opens cleanly in Xcode.

Files registered:
- Models: `Equipment.swift`, `Suggestion.swift`
- Services: `OpenExerciseDb.swift`, `ExerciseInstructions.swift`, `PRDetector.swift`, `ProgressAnalyzer.swift`
- Views/Home: `HomeWorkoutSheet.swift`, `SuggestionCard.swift`, `PlanSwapSheets.swift`
- Views/Calendar: `CalendarView.swift` (new group)
- Views/Theme: `Color+FitTrack.swift` (new group)
- Views/Workout: `PostWorkoutCelebration.swift`
- Resource: `free_exercise_db.json` in Copy Bundle Resources

Without these steps the new code won't compile and `OpenExerciseDb` will silently return only the hand-written fallback descriptions.

## Status legend
- 🔴 **Not started** — no iOS code exists for this feature
- 🟡 **Partial** — some code exists, gaps remain
- 🟢 **Live** — shipping equivalent on iOS

## Deferred items

### 1. HealthKit sync 🟢 SHIPPED
- HealthKit + Android Health Connect both shipped (see prior notes).

### 2. Supersets + RPE 🟢 SHIPPED

### 3. Watch / Wear OS companion 🔴 (deferred for both platforms)

### 4. Progress + History views 🟢 SHIPPED (iOS)

### 5. CSV export 🟢 SHIPPED (both platforms)

---

## Backlog from May 2026 Android batch (not yet on iOS)

### 6. Rest timer beep on completion 🟢 SHIPPED
- `ActiveWorkoutView.task(id: restTimerEndsAt)` sleeps until the endpoint then plays heavy haptic + `AudioServicesPlaySystemSound(1057)`. AVAudioSession is configured to `.playback` + `.mixWithOthers` so the beep routes through AirPods / Bluetooth output and doesn't pause music.

### 7. Rest timer resets on any new set completion 🟢 SHIPPED
- `autoStartRestTimer()` simplified — always sets `restTimerEndsAt = now + 90s`, no early-out.

### 8. Accordion exercise cards (only working one expanded) 🟢 SHIPPED
- `expandedExerciseID: UUID?` state in `ActiveWorkoutView`. Section header has a chevron + tap-to-toggle on the title area (action buttons stay independently tappable). Auto-advances via `autoAdvanceExpansion()` when the current exercise's working sets all have valid weight + reps. Set inputs / form viewer / Add Set button only render when the section is expanded.

### 9. Plan-aware "Train X More" suggestions 🟢 SHIPPED
- `Models/Suggestion.swift` + `Services/ProgressAnalyzer.swift` ported the simple Suggestion system to iOS, with the plan-aware frequency logic baked in from day one (no Android-style bug + fix sequence). Counts distinct workout days per muscle (not exercises). Uses planned frequency per muscle group as the threshold; falls back to 2x/week when no plan is set. Skips if user has <14 days of history. Honest "0 sessions in 4 weeks" / "3 times in 4 weeks" copy — no truncated per-week display.
- `Views/Home/SuggestionCard.swift` renders each tip; surfaced in `HomeView`'s new `improvementTipsSection`.

### 10. Suggestion → plan rewrite (two-step dialog) 🟢 SHIPPED
- `Views/Home/PlanSwapSheets.swift` — `PickRoutineSheet` (step 1, lists routine candidates with description + first 3 exercises + "In plan" tag) and `PickDaySheet` (step 2, lists existing plan days with the spacing-aware "Recommended" pick highlighted). `PlanSwapPlanner` provides pure functions for `prepareSwap` and `computeDayOptions`. Commit swap in `HomeView.commitSwap` just sets `userPlan.routine = newRoutine` and saves — SwiftData handles the rest. Confirmation alert with the swap summary (`Day 4: 'Push' → 'Day 2: Back'`).

### 11. Calendar tab 🟢 SHIPPED
- `Views/Calendar/CalendarView.swift` — month grid (Sunday-anchored), workout dots, missed-day shortfall rule mirroring the Android algorithm. Tap a day → summary card with `CalendarWorkoutRow` (inlined since `WorkoutHistoryRow` is file-private in HomeView). "Today" button in top bar. Swapped into `ContentView`'s `TabView` in place of Insights — Insights is still reachable from Home quickActions. My Plan's tab icon moved from `calendar` to `list.clipboard` so the two tabs are visually distinct.

### 12. Home: 3-stat row 🟢 SHIPPED
- `HomeView.statsCard` now shows Total / This Week / Volume in a row. `HomeStat` helper view in the same file. Volume formatted compactly (`12.3k`, `1.2M`). This-week boundary uses Monday-anchored ISO week math matching Android.

### 13. Semantic theme colors (success/warning/danger, dark-mode aware) 🟢 SHIPPED
- `Views/Theme/Color+FitTrack.swift` exposes `Color.fitTrackSuccess` / `.fitTrackWarning` / `.fitTrackDanger` using `UIColor(dynamicProvider:)` so they auto-adapt to dark mode. Same hex values as Android (light: 2E7D32/ED6C02/D32F2F, dark: 66BB6A/FFA726/EF5350). Migrated `InsightsView` (trend/score/fatigue/recommendation colors) and the new `CalendarView`. Remaining `.green`/`.orange`/`.red` in `ActiveWorkoutView` (plate calc) + `SettingsView` (one delete tint) + `HomeWorkoutSheet` (warning text) are minor — migrate as touched.

### 14. Settings kg/lbs as SegmentedButton 🟢 SHIPPED (already was)
- iOS already used `Picker.pickerStyle(.segmented)` on `weightUnit`. No change needed.

### 15. Equipment system 🟢 SHIPPED
- `Models/Equipment.swift` mirrors the Android enum. `Exercise.swift` gained `equipmentRaw` (default BARBELL) and a computed `equipment` accessor — SwiftData migrates additively, no schema migration code needed. `SeedData.retagAndExtendIfNeeded` runs on every launch when the catalog exists: idempotently re-tags built-in exercises whose equipment is wrong and inserts new catalog entries the user is missing. Custom user-created exercises are untouched.

### 16. Bodyweight catalog expansion 🟢 SHIPPED
- `SeedData.catalog` is now the source of truth (98 entries) — used for both fresh installs and the upgrade-path retag. Includes the 4 no-equipment biceps moves and the 4 wger-sourced no-equipment back moves.

### 17. ExerciseInstructions + free-exercise-db bundle 🟢 SHIPPED
- `Services/OpenExerciseDb.swift` (Swift twin of the Kotlin loader) reads the bundled `free_exercise_db.json` once on first access via `JSONSerialization` and caches a name/id → instructions map. `Services/ExerciseInstructions.swift` provides hand-written fallbacks. `ExerciseDescriptions.text(for:)` is the single entry point. Surfaced in `ActiveWorkoutView` inside the accordion body, above the Add Set button.
- **Xcode action needed** to actually ship: see the top of this file.

### 18. "Train at Home" flow 🟢 SHIPPED
- `Views/Home/HomeWorkoutSheet.swift` — modal sheet on Home. Muscle group picker + multi-select equipment chips. `@AppStorage("homeEquipment")` persists the user's selection (mirrors Android `EquipmentPrefs`).
- `HomeWorkoutGenerator.generate(group:equipment:context:)` is a pure function; primary-muscle first, then secondary; cap at 5; surfaces a warning hint when only secondary-muscle matches are available (Biceps + Bodyweight gets the same "Add a Pull-Up Bar / Band" message Android shows).
- `ActiveWorkoutView` gained `initialExercises: [Exercise]` parameter. Existing callsites still compile (default `= []`).
- Home screen has a "Home Workout" button (figure.mind.and.body SF Symbol, secondary card style) sitting below Quick Actions. Tap → sheet. On generate → push to ActiveWorkoutView via `navigationDestination(isPresented:)`.

### 19. PR detection + celebration 🟢 SHIPPED
- `Services/PRDetector.swift` — Epley 1RM (`weight * (1 + reps/30)`), excludes warmups, requires `delta > 0.5` to avoid float noise, skips first-time exercises.
- `Views/Workout/PostWorkoutCelebration.swift` — `.fullScreenCover` with bouncy trophy + delta cards + success haptic + 6s auto-dismiss. No confetti library (avoided dep for a 6-second flourish); spring animations carry the celebration feel.
- Wired into `ActiveWorkoutView.finishWorkout()`: detect PRs after save, hold screen for celebration if any, dismiss immediately otherwise.

### 20. ProgressChart animation + sparklines 🟢 SHIPPED
- `ExerciseProgressView` charts get `.interpolationMethod(.monotone)` + `.animation(.easeInOut(duration: 0.6), value: exercise.persistentModelID)` so swapping selected exercises animates the chart transition. `ExerciseSparkline` is a tiny inline `Chart` (56×24) shown as trailing content in each exercise picker row. Sparkline data precomputed via `.task(id: completedWorkouts.count)` so the LazyVStack doesn't recompute it per render.

### 21. Set completion redesign 🟢 SHIPPED
- `SetEntry` gained `isCompleted: Bool = false` (session-only). New leading `SetCompleteButton` (circle outline → filled green check). Auto-tick on when weight + reps are both valid for a non-warmup set; never auto-tick off — user toggles via the button. Row tints green (`Color.fitTrackSuccess.opacity(0.15)`) when completed via `.animation(.easeInOut(duration: 0.22), value: set.isCompleted)`. `UIImpactFeedbackGenerator(style: .medium)` haptic on the false→true transition.

## Currently iOS-only deficiencies (not Android-shipped, just worth noting)

- Brand naming inconsistency in Info.plist + HomeView still says "SmartGym Tracker" (separate spawned task).

## Update protocol

After every Android-only PR, append the corresponding iOS work item here.
After every iOS catch-up PR, mark items 🟢 and remove them from this file (keep the file lean).
