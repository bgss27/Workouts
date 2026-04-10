import SwiftUI
import SwiftData

struct InsightsView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var proManager: ProManager
    @State private var allInsights: [MuscleInsightData] = []
    @State private var selectedInsight: MuscleInsightData?
    @State private var isLoading = true

    var body: some View {
        NavigationStack {
            if !proManager.isPro {
                proLockedView
            } else {
                insightsContent
            }
        }
    }

    private var proLockedView: some View {
        VStack(spacing: 16) {
            Image(systemName: "lock.fill").font(.system(size: 64)).opacity(0.4)
            Text("ML Insights").font(.title2.bold())
            Text("AI-powered muscle analysis with plateau detection and personalized recommendations")
                .font(.caption).multilineTextAlignment(.center).opacity(0.6)
            NavigationLink("Upgrade to Pro") { UpgradeView() }
                .buttonStyle(.borderedProminent)
        }
        .padding(32)
        .navigationTitle("Insights")
    }

    private var insightsContent: some View {
        ScrollView {
            VStack(spacing: 12) {
                if isLoading {
                    ProgressView("Running ML analysis...")
                        .padding(32)
                } else if let insight = selectedInsight {
                    detailView(insight)
                } else if !allInsights.isEmpty {
                    overviewList
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "brain.head.profile").font(.system(size: 48)).opacity(0.3)
                        Text("No data for analysis").font(.headline)
                        Text("Complete a few workouts first").font(.caption).opacity(0.6)
                    }
                    .padding(32)
                }
            }
            .padding()
        }
        .navigationTitle(selectedInsight != nil ? selectedInsight!.muscleGroup.displayName : "Insights")
        .toolbar {
            if selectedInsight != nil {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Back") { selectedInsight = nil }
                }
            }
        }
        .onAppear { loadInsights() }
    }

    private var overviewList: some View {
        VStack(spacing: 8) {
            Text("Muscle Group Scores").font(.headline).frame(maxWidth: .infinity, alignment: .leading)
            ForEach(allInsights) { insight in
                Button { selectedInsight = insight } label: {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(insight.muscleGroup.displayName).font(.subheadline.bold())
                            HStack(spacing: 12) {
                                Text(insight.strengthTrend.rawValue).font(.caption)
                                    .foregroundColor(trendColor(insight.strengthTrend))
                                Text("\(String(format: "%.0f", insight.weeklyVolumeSets)) sets/wk")
                                    .font(.caption).opacity(0.6)
                                if insight.isPlateaued {
                                    Text("Plateaued").font(.caption).foregroundColor(.orange)
                                }
                            }
                            if !insight.recommendations.isEmpty {
                                Text("\(insight.recommendations.count) recommendations")
                                    .font(.caption2).foregroundColor(.accentColor)
                            }
                        }
                        Spacer()
                        VStack {
                            Text("\(Int(insight.overallScore))").font(.title2.bold())
                                .foregroundColor(scoreColor(insight.overallScore))
                            Text("Score").font(.caption2).opacity(0.5)
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func detailView(_ insight: MuscleInsightData) -> some View {
        VStack(spacing: 12) {
            // Score card
            HStack(spacing: 20) {
                scoreItem("Score", "\(Int(insight.overallScore))/100", scoreColor(insight.overallScore))
                scoreItem("Fatigue", "\(Int(insight.fatigueScore))%", fatigueColor(insight.fatigueScore))
                scoreItem("Balance", "\(Int(insight.balanceScore))/100", scoreColor(insight.balanceScore))
                scoreItem("Sets/Wk", String(format: "%.0f", insight.weeklyVolumeSets), .accentColor)
            }
            .padding()
            .background(Color(.secondarySystemBackground))
            .cornerRadius(12)

            // Phase & Recovery
            HStack {
                Label(insight.trainingPhase.rawValue, systemImage: "figure.strengthtraining.traditional")
                Spacer()
                Label(insight.recoveryStatus.rawValue, systemImage: "heart.fill")
                    .foregroundColor(fatigueColor(insight.fatigueScore))
            }
            .font(.caption)
            .padding()
            .background(Color(.secondarySystemBackground))
            .cornerRadius(12)

            if insight.isPlateaued {
                HStack {
                    Image(systemName: "exclamationmark.triangle.fill").foregroundColor(.orange)
                    Text("Plateau detected (\(insight.plateauDurationSessions) sessions)")
                        .font(.caption)
                }
                .padding()
                .background(Color.orange.opacity(0.1))
                .cornerRadius(12)
            }

            // Recommendations
            if !insight.recommendations.isEmpty {
                Text("ML Recommendations").font(.headline).frame(maxWidth: .infinity, alignment: .leading)
                ForEach(insight.recommendations) { rec in
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: recIcon(rec.type))
                            .foregroundColor(recColor(rec.type))
                        VStack(alignment: .leading, spacing: 2) {
                            HStack {
                                Text(rec.title).font(.subheadline.bold())
                                Spacer()
                                Text("\(Int(rec.confidence * 100))%").font(.caption2).opacity(0.5)
                            }
                            Text(rec.description).font(.caption).opacity(0.7)
                        }
                    }
                    .padding()
                    .background(recColor(rec.type).opacity(0.08))
                    .cornerRadius(12)
                }
            }

            // Exercise insights
            if !insight.exerciseInsights.isEmpty {
                Text("Per-Exercise Analysis").font(.headline).frame(maxWidth: .infinity, alignment: .leading)
                ForEach(insight.exerciseInsights) { ex in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(ex.exerciseName).font(.subheadline.bold())
                            Spacer()
                            Image(systemName: trendIcon(ex.strengthTrend))
                                .foregroundColor(trendColor(ex.strengthTrend))
                        }
                        HStack {
                            Text("1RM: \(String(format: "%.1f", ex.estimated1RM))kg").font(.caption)
                            Text("Best: \(String(format: "%.1f", ex.bestWeight))kg").font(.caption)
                        }
                        if ex.rSquared > 0.2 && ex.predicted1RMNext > ex.estimated1RM {
                            Text("Predicted: \(String(format: "%.1f", ex.predicted1RMNext))kg (\(Int(ex.rSquared * 100))%)")
                                .font(.caption2).foregroundColor(.green)
                        }
                        if ex.isPlateaued { Text("Plateaued").font(.caption2).foregroundColor(.orange) }
                        Text("\(ex.sessionCount) sessions").font(.caption2).opacity(0.5)
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                }
            }
        }
    }

    private func scoreItem(_ label: String, _ value: String, _ color: Color) -> some View {
        VStack {
            Text(value).font(.headline).foregroundColor(color)
            Text(label).font(.caption2).opacity(0.6)
        }
        .frame(maxWidth: .infinity)
    }

    private func loadInsights() {
        let engine = MlAnalysisEngine(context: modelContext)
        allInsights = engine.analyzeAllMuscleGroups()
        isLoading = false
    }

    private func trendColor(_ t: TrendDirection) -> Color {
        switch t {
        case .improving: return .green
        case .slightlyImproving: return .green.opacity(0.7)
        case .declining: return .red
        case .slightlyDeclining: return .red.opacity(0.7)
        default: return .orange
        }
    }

    private func trendIcon(_ t: TrendDirection) -> String {
        switch t {
        case .improving, .slightlyImproving: return "arrow.up.right"
        case .declining, .slightlyDeclining: return "arrow.down.right"
        default: return "arrow.right"
        }
    }

    private func scoreColor(_ s: Double) -> Color { s >= 70 ? .green : s >= 40 ? .orange : .red }
    private func fatigueColor(_ f: Double) -> Color { f < 25 ? .green : f < 50 ? .orange : .red }

    private func recIcon(_ t: RecommendationType) -> String {
        switch t {
        case .increaseWeight: return "arrow.up.right"
        case .increaseVolume: return "plus.circle"
        case .increaseFrequency: return "repeat"
        case .deload: return "arrow.down.right"
        case .changeExercise: return "arrow.triangle.2.circlepath"
        case .periodizationShift: return "arrow.2.squarepath"
        case .muscleBalance: return "scale.3d"
        case .recovery: return "bed.double.fill"
        }
    }

    private func recColor(_ t: RecommendationType) -> Color {
        switch t {
        case .increaseWeight: return .green
        case .increaseVolume, .increaseFrequency, .periodizationShift: return .blue
        case .deload, .muscleBalance: return .orange
        case .changeExercise: return .teal
        case .recovery: return .red
        }
    }
}
