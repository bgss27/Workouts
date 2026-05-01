import SwiftUI
import SwiftData

struct ContentView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var proManager: ProManager
    @State private var selectedTab = 0
    @State private var hasSeeded = false

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView()
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }
                .tag(0)

            MyPlanView()
                .tabItem {
                    Label("My Plan", systemImage: "calendar")
                }
                .tag(1)

            ExercisesView()
                .tabItem {
                    Label("Exercises", systemImage: "dumbbell.fill")
                }
                .tag(2)

            InsightsView()
                .tabItem {
                    Label("Insights", systemImage: "brain.head.profile")
                }
                .tag(3)

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
                .tag(4)
        }
        .onAppear {
            if !hasSeeded {
                SeedData.seedIfNeeded(context: modelContext)
                hasSeeded = true
            }
        }
    }
}
