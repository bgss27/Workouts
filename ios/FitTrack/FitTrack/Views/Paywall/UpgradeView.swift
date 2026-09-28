import SwiftUI

private enum Tier: String, CaseIterable {
    case annual, lifetime, monthly
}

struct UpgradeView: View {
    @EnvironmentObject var proManager: ProManager
    @State private var selectedTier: Tier = .annual

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                header

                if proManager.isPro {
                    Label("Pro entitlement active", systemImage: "checkmark.circle.fill")
                        .foregroundColor(.green)
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.green.opacity(0.1))
                        .cornerRadius(12)
                }

                featureList

                if !proManager.isPro {
                    tierPicker
                    purchaseCTA
                    finePrint

                    Button("Restore Purchases") {
                        Task { await proManager.restorePurchases() }
                    }
                    .font(.caption)
                }

                #if DEBUG
                debugToggle
                #endif
            }
            .padding()
        }
        .navigationTitle("Upgrade")
    }

    private var header: some View {
        VStack(spacing: 8) {
            Image(systemName: "star.fill")
                .font(.system(size: 60))
                .foregroundColor(.yellow)
            Text(proManager.isPro ? "You're a Pro!" : "Unlock FitTrack Pro")
                .font(.title.bold())
            Text(proManager.isPro
                ? "You have access to all features"
                : "Get ML-powered insights, all programs, and more")
                .font(.subheadline)
                .opacity(0.7)
                .multilineTextAlignment(.center)
        }
    }

    // Only features that are actually live ship in this list.
    // Add a new row here when (and only when) the corresponding gate is implemented.
    // Avoid aspirational features — Apple Guideline 5.0 / 2.3.1 rejection risk.
    private var featureList: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("What's included in Pro").font(.headline)
            featureRow(icon: "brain.head.profile", feature: .insights)
            featureRow(icon: "chart.xyaxis.line", feature: .fullHistory)
            featureRow(icon: "heart.fill", feature: .integrations)
            featureRow(icon: "square.and.arrow.up", feature: .export)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var tierPicker: some View {
        VStack(spacing: 12) {
            Text("Choose your plan")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)

            tierCard(
                tier: .annual,
                title: "Annual",
                price: proManager.yearlyPrice,
                pricePeriod: "/ year",
                subtitle: proManager.yearlyTrialDuration.map { "\($0) free, then auto-renews" }
                    ?? "Best for committed lifters",
                badge: "Most Popular",
                emphasizeBadge: false
            )

            tierCard(
                tier: .lifetime,
                title: "Lifetime",
                price: proManager.lifetimePrice,
                pricePeriod: "once",
                subtitle: lifetimeSubtitle,
                badge: "Best Value",
                emphasizeBadge: proManager.founderLifetimePriceActive
            )

            tierCard(
                tier: .monthly,
                title: "Monthly",
                price: proManager.monthlyPrice,
                pricePeriod: "/ month",
                subtitle: "Cancel anytime",
                badge: nil,
                emphasizeBadge: false
            )
        }
    }

    private var purchaseCTA: some View {
        Button {
            Task {
                switch selectedTier {
                case .annual:   _ = await proManager.purchaseYearly()
                case .lifetime: _ = await proManager.purchaseLifetime()
                case .monthly:  _ = await proManager.purchaseMonthly()
                }
            }
        } label: {
            Label(ctaLabel, systemImage: "star.fill")
                .frame(maxWidth: .infinity)
                .padding()
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
    }

    private var ctaLabel: String {
        switch selectedTier {
        case .lifetime: return "Get Lifetime Access"
        case .annual:
            if let duration = proManager.yearlyTrialDuration {
                return "Start \(duration) Free Trial"
            }
            return "Subscribe Now"
        case .monthly: return "Subscribe Now"
        }
    }

    private var finePrint: some View {
        Text(finePrintText)
            .font(.caption2)
            .opacity(0.4)
            .multilineTextAlignment(.center)
    }

    private var finePrintText: String {
        switch selectedTier {
        case .lifetime:
            return "One-time purchase. No subscription. Managed through App Store."
        case .annual:
            if let duration = proManager.yearlyTrialDuration {
                return "Free for \(duration), then \(proManager.yearlyPrice). Cancel anytime in App Store subscriptions."
            }
            return "Cancel anytime. Subscription auto-renews. Managed through App Store."
        case .monthly:
            return "Cancel anytime. Subscription auto-renews. Managed through App Store."
        }
    }

    #if DEBUG
    private var debugToggle: some View {
        VStack(spacing: 0) {
            Divider()
            HStack {
                VStack(alignment: .leading) {
                    Text("Debug: Toggle Pro").font(.subheadline.bold())
                    Text("Debug build only").font(.caption2).opacity(0.5)
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
    }
    #endif

    private func featureRow(icon: String, feature: Feature) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .foregroundColor(.accentColor)
                .frame(width: 24)
            VStack(alignment: .leading) {
                Text(feature.displayName).font(.subheadline.bold())
                Text(feature.summary).font(.caption).opacity(0.6)
            }
        }
    }

    private func tierCard(
        tier: Tier,
        title: String,
        price: String,
        pricePeriod: String,
        subtitle: String,
        badge: String?,
        emphasizeBadge: Bool
    ) -> some View {
        let isSelected = selectedTier == tier
        return Button {
            selectedTier = tier
        } label: {
            HStack(alignment: .center, spacing: 12) {
                Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                    .foregroundColor(.accentColor)

                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(title).font(.subheadline.bold())
                        if let badge {
                            tierBadge(text: badge, accent: emphasizeBadge)
                        }
                    }
                    Text(subtitle).font(.caption).opacity(0.6)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text(price).font(.subheadline.bold())
                    Text(pricePeriod)
                        .font(.caption2)
                        .opacity(0.5)
                }
            }
            .padding()
            .background(isSelected ? Color.accentColor.opacity(0.10) : Color(.secondarySystemBackground))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Color.accentColor : Color.clear, lineWidth: 2)
            )
        }
        .buttonStyle(.plain)
    }

    private var lifetimeSubtitle: String {
        guard proManager.founderLifetimePriceActive else {
            return "One-time payment, lifetime access"
        }
        if let endsAt = proManager.founderLifetimeEndsAt {
            let formatter = DateFormatter()
            formatter.dateStyle = .long
            formatter.timeStyle = .none
            return "Founder pricing — through \(formatter.string(from: endsAt))"
        }
        return "Founder pricing — limited time"
    }

    private func tierBadge(text: String, accent: Bool) -> some View {
        Text(text)
            .font(.caption2.bold())
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .background(accent ? Color.orange : Color.accentColor)
            .foregroundColor(.white)
            .cornerRadius(8)
    }
}
