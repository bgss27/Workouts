import SwiftUI

struct UpgradeView: View {
    @EnvironmentObject var proManager: ProManager
    @State private var selectedPlan = "yearly"

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header
                VStack(spacing: 8) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.yellow)
                    Text(proManager.isPro ? "You're a Pro!" : "Unlock SmartGym Log Pro")
                        .font(.title.bold())
                    Text(proManager.isPro ? "You have access to all features" : "ML insights, all programs, and more")
                        .font(.subheadline).opacity(0.7)
                }

                if proManager.isPro {
                    Label("Pro subscription active", systemImage: "checkmark.circle.fill")
                        .foregroundColor(.green)
                        .padding()
                        .background(Color.green.opacity(0.1))
                        .cornerRadius(12)
                }

                // Features
                VStack(alignment: .leading, spacing: 16) {
                    Text("What's included").font(.headline)
                    featureRow(icon: "brain.head.profile", title: "ML Insights", desc: "AI-powered muscle analysis with predictions")
                    featureRow(icon: "dumbbell.fill", title: "All Programs", desc: "3-day and 5-day workout programs")
                    featureRow(icon: "calendar", title: "Unlimited Plans", desc: "Save any program to your plan")
                    featureRow(icon: "chart.xyaxis.line", title: "Advanced Charts", desc: "Detailed progress tracking")
                    featureRow(icon: "square.and.arrow.up", title: "Data Export", desc: "Export workout history to CSV")
                }

                if !proManager.isPro {
                    // Plan selection
                    VStack(spacing: 12) {
                        Text("Choose your plan").font(.headline)

                        planCard(id: "yearly", label: "Yearly", price: proManager.yearlyPrice, badge: "Save 50%")
                        planCard(id: "monthly", label: "Monthly", price: proManager.monthlyPrice, badge: nil)
                    }

                    Button {
                        Task {
                            if selectedPlan == "yearly" {
                                await proManager.purchaseYearly()
                            } else {
                                await proManager.purchaseMonthly()
                            }
                        }
                    } label: {
                        Label("Subscribe Now", systemImage: "star.fill")
                            .frame(maxWidth: .infinity)
                            .padding()
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)

                    Text("Cancel anytime. Managed through App Store.")
                        .font(.caption2).opacity(0.4)
                        .multilineTextAlignment(.center)

                    Button("Restore Purchases") {
                        Task { await proManager.restorePurchases() }
                    }
                    .font(.caption)
                }

                // Debug toggle
                Divider()
                HStack {
                    VStack(alignment: .leading) {
                        Text("Debug: Toggle Pro").font(.subheadline.bold())
                        Text("For testing only").font(.caption2).opacity(0.5)
                    }
                    Spacer()
                    Toggle("", isOn: Binding(
                        get: { proManager.isPro },
                        set: { _ in proManager.debugTogglePro() }
                    ))
                }
                .padding()
                .background(Color.red.opacity(0.05))
                .cornerRadius(12)
            }
            .padding()
        }
        .navigationTitle("Upgrade")
    }

    private func featureRow(icon: String, title: String, desc: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon).foregroundColor(.accentColor).frame(width: 24)
            VStack(alignment: .leading) {
                Text(title).font(.subheadline.bold())
                Text(desc).font(.caption).opacity(0.6)
            }
        }
    }

    private func planCard(id: String, label: String, price: String, badge: String?) -> some View {
        Button { selectedPlan = id } label: {
            HStack {
                Image(systemName: selectedPlan == id ? "largecircle.fill.circle" : "circle")
                    .foregroundColor(.accentColor)
                Text(label).font(.subheadline.bold())
                if let badge {
                    Text(badge).font(.caption2.bold())
                        .padding(.horizontal, 8).padding(.vertical, 2)
                        .background(Color.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
                Spacer()
                Text(price).font(.subheadline.bold())
            }
            .padding()
            .background(selectedPlan == id ? Color.accentColor.opacity(0.1) : Color(.secondarySystemBackground))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(selectedPlan == id ? Color.accentColor : .clear, lineWidth: 2)
            )
        }
        .buttonStyle(.plain)
    }
}
