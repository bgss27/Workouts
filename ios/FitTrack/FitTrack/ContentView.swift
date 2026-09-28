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
                    Label("My Plan", systemImage: "list.clipboard")
                }
                .tag(1)

            CalendarView()
                .tabItem {
                    Label("Calendar", systemImage: "calendar")
                }
                .tag(2)

            ExercisesView()
                .tabItem {
                    Label("Exercises", systemImage: "dumbbell.fill")
                }
                .tag(3)

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
                .tag(4)
            // Insights stays reachable from Home quickActions; not a tab now
            // since Calendar took its slot in the bottom nav (matches Android).
        }
        .onAppear {
            if !hasSeeded {
                SeedData.seedIfNeeded(context: modelContext)
                hasSeeded = true
            }
        }
    }
}
