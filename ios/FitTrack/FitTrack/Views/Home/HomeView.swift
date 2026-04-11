import SwiftUI
import SwiftData

struct HomeView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var proManager: ProManager
    @Query(sort: \Workout.startTime, order: .reverse) private var workouts: [Workout]
    @Query private var userPlans: [UserPlan]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // My Plan preview
                    if !userPlans.isEmpty {
                        myPlanSection
                    } else {
                        setupPlanCard
                    }

                    // Pro upgrade banner
                    if !proManager.isPro {
                        NavigationLink(destination: UpgradeView()) {
                            HStack {
                                Image(systemName: "star.fill")
                                    .foregroundColor(.yellow)
                                VStack(alignment: .leading) {
                                    Text("Upgrade to Pro").font(.subheadline.bold())
                                    Text("Unlock ML insights, all programs").font(.caption).opacity(0.7)
                                }
                                Spacer()
                                Image(systemName: "chevron.right").opacity(0.5)
                            }
                            .padding()
                            .background(.ultraThinMaterial)
                            .cornerRadius(12)
                        }
                        .buttonStyle(.plain)
                    }

                    // Stats
                    statsCard

                    // Quick actions
                    quickActions

                    // Recent workouts
                    recentWorkoutsSection
                }
                .padding()
            }
            .navigationTitle("SmartGym Log")
        }
    }

    private var myPlanSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("My Plan").font(.headline)
                if let prog = userPlans.first?.programName {
                    Text(prog).font(.caption).foregroundColor(.accentColor)
                }
                Spacer()
                NavigationLink("View All") { MyPlanView() }
                    .font(.caption)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(userPlans.sorted { $0.dayOrder < $1.dayOrder }, id: \.self) { plan in
                        NavigationLink(destination: ActiveWorkoutView(routine: plan.routine)) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(plan.programName != nil ? "Day \(plan.dayOrder)" : "Workout")
                                    .font(.caption).foregroundColor(.accentColor)
                                Text(plan.routine?.name ?? "Workout")
                                    .font(.subheadline.bold())
                                    .lineLimit(1)
                                Text("\(plan.routine?.exercises.count ?? 0) exercises")
                                    .font(.caption2).opacity(0.6)
                                Label("Start", systemImage: "play.fill")
                                    .font(.caption.bold())
                                    .padding(.horizontal, 12).padding(.vertical, 6)
                                    .background(Color.accentColor)
                                    .foregroundColor(.white)
                                    .cornerRadius(8)
                            }
                            .padding()
                            .frame(width: 170)
                            .background(Color(.secondarySystemBackground))
                            .cornerRadius(12)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var setupPlanCard: some View {
        NavigationLink(destination: RoutineListView()) {
            HStack {
                Image(systemName: "calendar")
                VStack(alignment: .leading) {
                    Text("Set up your workout plan").font(.subheadline.bold())
                    Text("Choose a 3-day or 5-day program").font(.caption).opacity(0.7)
                }
                Spacer()
                Image(systemName: "chevron.right").opacity(0.5)
            }
            .padding()
            .background(Color(.secondarySystemBackground))
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }

    private var statsCard: some View {
        VStack {
            Text("Your Progress").font(.subheadline.bold())
            Text("\(workouts.filter(\.isCompleted).count)")
                .font(.largeTitle.bold())
            Text("Workouts").font(.caption).opacity(0.6)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color.accentColor.opacity(0.1))
        .cornerRadius(12)
    }

    private var quickActions: some View {
        HStack(spacing: 12) {
            NavigationLink(destination: ActiveWorkoutView(routine: nil)) {
                quickActionCard(icon: "dumbbell.fill", label: "Workout")
            }
            NavigationLink(destination: InsightsView()) {
                quickActionCard(icon: "brain.head.profile", label: "Insights")
            }
            NavigationLink(destination: RoutineListView()) {
                quickActionCard(icon: "list.bullet", label: "Routines")
            }
        }
        .buttonStyle(.plain)
    }

    private func quickActionCard(icon: String, label: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
            Text(label).font(.caption2)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(10)
    }

    private var recentWorkoutsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Recent Workouts").font(.headline)
            if workouts.filter(\.isCompleted).isEmpty {
                Text("No workouts yet. Start your first one!")
                    .font(.caption).opacity(0.6)
                    .frame(maxWidth: .infinity).padding(.vertical, 32)
            } else {
                ForEach(workouts.filter(\.isCompleted).prefix(5), id: \.self) { workout in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(workout.startTime, style: .date).font(.subheadline.bold())
                            Text("\(workout.exercises.count) exercises")
                                .font(.caption).opacity(0.6)
                        }
                        Spacer()
                        if let mins = workout.durationMinutes {
                            Text("\(mins) min").font(.caption).foregroundColor(.accentColor)
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                }
            }
        }
    }
}
