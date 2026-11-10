import SwiftUI
import SwiftData

@main
struct FitTrackApp: App {
    @StateObject private var proManager = ProManager()
    @StateObject private var healthKit = HealthKitService()
    @AppStorage(ThemeMode.storageKey) private var themeModeRaw = ThemeMode.defaultMode.rawValue

    init() {
        FitAppearance.apply()
        RestTimerNotifier.shared.install()
    }

    var sharedModelContainer: ModelContainer = {
        let schema = Schema([
            Exercise.self,
            Workout.self,
            WorkoutExercise.self,
            WorkoutSet.self,
            Routine.self,
            RoutineExercise.self,
            UserPlan.self,
            BodyMetric.self,
            ProgressPhoto.self
        ])
        // Copy the store aside before a schema change touches it; see StoreBackup.
        StoreBackup.backupIfSchemaChanged(modelNames: [
            "Exercise", "Workout", "WorkoutExercise", "WorkoutSet",
            "Routine", "RoutineExercise", "UserPlan", "BodyMetric", "ProgressPhoto",
            // Bumped whenever attributes are added so the backup fires even
            // though the model list is unchanged.
            "schema:v3",
        ])
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        do {
            let container = try ModelContainer(for: schema, configurations: [config])
            return container
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(proManager)
                .environmentObject(healthKit)
                .preferredColorScheme((ThemeMode(rawValue: themeModeRaw) ?? ThemeMode.defaultMode).colorScheme)
                .tint(Color.fitPrimary)
        }
        .modelContainer(sharedModelContainer)
    }
}
