import SwiftUI
import SwiftData

@main
struct FitTrackApp: App {
    @StateObject private var proManager = ProManager()

    var sharedModelContainer: ModelContainer = {
        let schema = Schema([
            Exercise.self,
            Workout.self,
            WorkoutExercise.self,
            WorkoutSet.self,
            Routine.self,
            RoutineExercise.self,
            UserPlan.self
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
        }
        .modelContainer(sharedModelContainer)
    }
}
