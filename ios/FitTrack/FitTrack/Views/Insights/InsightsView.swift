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
            ScrollView {
                VStack(spacing: 12) {
                    if isLoading {
                        ProgressView("Running ML analysis...")
                            .padding(32)
                    } else if let insight = selectedInsight {
                        detailView(insight)
                        if !proManager.isPro {
                            paywallFooter
                        }
                    } else if !allInsights.isEmpty {
                        overviewList
                    } else {
                        emptyView
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
    }

    private var emptyView: some View {
        VStack(spacing: 12) {
            Image(systemName: "brain.head.profile").font(.system(size: 48)).opacity(0.3)
            Text("No data for analysis").font(.headline)
            Text("Complete a few workouts first").font(.caption).opacity(0.6)
        }
        .padding(32)
    }

    private var overviewList: some View {
        let visibleInsights = proManager.isPro ? allInsights : Array(allInsights.prefix(1))
        let lockedInsights = proManager.isPro ? [] : Array(allInsights.dropFirst())

        return VStack(spacing: 8) {
            Text("Muscle Group Scores")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)

            ForEach(visibleInsights) { insight in
                overviewCard(insight)
            }

            if !lockedInsights.isEmpty {
                paywallTeaser(lockedNames: lockedInsights.map { $0.muscleGroup.displayName })
            }
        }
    }

    private func overviewCard(_ insight: MuscleInsightData) -> some View {
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
                            Text("Plateaued").font(.caption).foregroundColor(.fitTrackWarning)
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

    private func paywallTeaser(lockedNames: [String]) -> some View {
        let preview = lockedNames.prefix(4).joined(separator: ", ")
        let suffix = lockedNames.count > 4 ? ", and \(lockedNames.count - 4) more" : ""
        return VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "lock.fill").foregroundColor(.accentColor)
                Text("\(lockedNames.count) more insights with Pro")
                    .font(.subheadline.bold())
            }
            Text("Unlock plateau detection, frequency analysis, and personalized recommendations for \(preview)\(suffix).")
                .font(.caption)
                .opacity(0.7)
            NavigationLink {
                UpgradeView()
            } label: {
                Label("Unlock with Pro", systemImage: "star.fill")
                    .font(.caption.bold())
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.accentColor.opacity(0.1))
        .cornerRadius(12)
    }

    private var paywallFooter: some View {
        VStack(spacing: 8) {
            Text("Like this preview?").font(.subheadline.bold())
            Text("Unlock insights for every muscle group with Pro.")
                .font(.caption).opacity(0.7)
                .multilineTextAlignment(.center)
            NavigationLink {
                UpgradeView()
            } label: {
                Label("Upgrade to Pro", systemImage: "star.fill")
                    .font(.caption.bold())
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color.accentColor.opacity(0.1))
        .cornerRadius(12)
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
                    Image(systemName: "exclamationmark.triangle.fill").foregroundColor(.fitTrackWarning)
                    Text("Plateau detected (\(insight.plateauDurationSessions) sessions)")
                        .font(.caption)
                }
                .padding()
                .background(Color.fitTrackWarning.opacity(0.1))
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
                                .font(.caption2).foregroundColor(.fitTrackSuccess)
                        }
                        if ex.isPlateaued { Text("Plateaued").font(.caption2).foregroundColor(.fitTrackWarning) }
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
        case .improving: return .fitTrackSuccess
        case .slightlyImproving: return .fitTrackSuccess.opacity(0.7)
        case .declining: return .fitTrackDanger
        case .slightlyDeclining: return .fitTrackDanger.opacity(0.7)
        default: return .fitTrackWarning
        }
    }

    private func trendIcon(_ t: TrendDirection) -> String {
        switch t {
        case .improving, .slightlyImproving: return "arrow.up.right"
        case .declining, .slightlyDeclining: return "arrow.down.right"
        default: return "arrow.right"
        }
    }

    private func scoreColor(_ s: Double) -> Color { s >= 70 ? .fitTrackSuccess : s >= 40 ? .fitTrackWarning : .fitTrackDanger }
    private func fatigueColor(_ f: Double) -> Color { f < 25 ? .fitTrackSuccess : f < 50 ? .fitTrackWarning : .fitTrackDanger }

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
        case .increaseWeight: return .fitTrackSuccess
        case .increaseVolume, .increaseFrequency, .periodizationShift: return .blue
        case .deload, .muscleBalance: return .fitTrackWarning
        case .changeExercise: return .teal
        case .recovery: return .fitTrackDanger
        }
    }
}
