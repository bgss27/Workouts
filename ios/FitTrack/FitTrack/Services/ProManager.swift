import Foundation
import HealthKit
import StoreKit

/// Capabilities that may be gated behind the Pro tier. Mirror of Android's
/// `FeatureGate` enum — keep the two in sync so the support story doesn't
/// diverge across platforms.
enum Feature: String, CaseIterable {
    case insights
    case unlimitedProgramSlots
    case unlimitedRoutines
    case fullHistory
    case bodyMetrics
    case export
    case integrations
    case watch

    var displayName: String {
        switch self {
        case .insights: return "ML Insights"
        case .unlimitedProgramSlots: return "Multiple Active Programs"
        case .unlimitedRoutines: return "Unlimited Custom Routines"
        case .fullHistory: return "Full History"
        case .bodyMetrics: return "Body Metrics"
        case .export: return "Data Export"
        case .integrations: return "Health Sync"
        case .watch: return "Watch Companion"
        }
    }

    var summary: String {
        switch self {
        case .insights:
            return "Plateau detection, frequency analysis, and personalized recommendations"
        case .unlimitedProgramSlots:
            return "Run more than one workout program at the same time"
        case .unlimitedRoutines:
            return "Build and save as many custom routines as you want"
        case .fullHistory:
            return "View workouts and progress charts beyond the last 30 days"
        case .bodyMetrics:
            return "Log body weight, measurements, and progress photos"
        case .export:
            return "Export workout history to CSV"
        case .integrations:
            return "Two-way sync with Apple Health"
        case .watch:
            return "Quick-log sets and rest timer on Apple Watch"
        }
    }
}

@MainActor
class ProManager: ObservableObject {
    @Published private(set) var isPro: Bool {
        didSet { UserDefaults.standard.set(isPro, forKey: Self.proCacheKey) }
    }

    /// Whether the launch-window founder lifetime price is currently being shown.
    /// Auto-derived from ``founderLifetimeEndDate`` — flips off automatically the
    /// first time the upgrade screen is opened after the cutoff.
    var founderLifetimePriceActive: Bool {
        Date() < Self.founderLifetimeEndDate
    }

    /// The date at which the founder window ends, or nil once it has passed.
    var founderLifetimeEndsAt: Date? {
        founderLifetimePriceActive ? Self.founderLifetimeEndDate : nil
    }

    /// Cutoff for founder lifetime pricing. Update before each launch / extension.
    /// Currently 2026-06-30 23:59:59 UTC.
    private static let founderLifetimeEndDate: Date = {
        var components = DateComponents()
        components.year = 2026
        components.month = 6
        components.day = 30
        components.hour = 23
        components.minute = 59
        components.second = 59
        components.timeZone = TimeZone(identifier: "UTC")
        return Calendar(identifier: .gregorian).date(from: components) ?? .distantFuture
    }()

    static let monthlyProductId = "fittrack_pro_monthly"
    static let yearlyProductId = "fittrack_pro_yearly"
    static let lifetimeProductId = "fittrack_pro_lifetime"

    private static let proCacheKey = "fittrack_is_pro"
    private static let allProductIds: [String] = [
        monthlyProductId, yearlyProductId, lifetimeProductId,
    ]
    private static let entitlingProductIds: Set<String> = Set(allProductIds)

    private var products: [Product] = []
    private var updateListenerTask: Task<Void, Error>?

    /// True when the App Store says this user is eligible for the introductory
    /// offer (free trial) on the annual subscription. Refreshed alongside
    /// product loading and after each entitlement change.
    @Published private(set) var isEligibleForYearlyTrial: Bool = false

    init() {
        self.isPro = UserDefaults.standard.bool(forKey: Self.proCacheKey)
        updateListenerTask = listenForTransactions()
        Task { await loadProducts() }
        Task { await refreshEntitlements() }
    }

    deinit { updateListenerTask?.cancel() }

    // MARK: - Public entitlement API

    /// True when the given capability is unlocked for the current user.
    func isUnlocked(_ feature: Feature) -> Bool {
        // Every gate currently maps to "any Pro entitlement". When tier
        // differentiation lands (e.g. Watch only on annual+), branch here.
        _ = feature
        return isPro
    }

    var monthlyProduct: Product? { products.first { $0.id == Self.monthlyProductId } }
    var yearlyProduct: Product? { products.first { $0.id == Self.yearlyProductId } }
    var lifetimeProduct: Product? { products.first { $0.id == Self.lifetimeProductId } }

    var monthlyPrice: String { monthlyProduct?.displayPrice ?? "$3.99" }
    var yearlyPrice: String { yearlyProduct?.displayPrice ?? "$29.99" }
    var lifetimePrice: String {
        lifetimeProduct?.displayPrice ?? (founderLifetimePriceActive ? "$59.99" : "$79.99")
    }

    /// Trial duration string (e.g. "7 days") for the yearly subscription's
    /// introductory offer when one is configured in App Store Connect AND the
    /// user is eligible. nil otherwise — UI uses this to decide whether to
    /// show "Start free trial" copy.
    var yearlyTrialDuration: String? {
        guard isEligibleForYearlyTrial else { return nil }
        guard let intro = yearlyProduct?.subscription?.introductoryOffer else { return nil }
        guard intro.paymentMode == .freeTrial else { return nil }
        return formatPeriod(intro.period)
    }

    func purchaseMonthly() async -> Bool {
        await purchase(productId: Self.monthlyProductId)
    }

    func purchaseYearly() async -> Bool {
        await purchase(productId: Self.yearlyProductId)
    }

    func purchaseLifetime() async -> Bool {
        await purchase(productId: Self.lifetimeProductId)
    }

    func restorePurchases() async {
        try? await AppStore.sync()
        await refreshEntitlements()
    }

    func loadProducts() async {
        do {
            products = try await Product.products(for: Self.allProductIds)
            await refreshTrialEligibility()
        } catch {
            print("ProManager: failed to load products — \(error)")
        }
    }

    /// Re-check whether this user is eligible for the annual trial.
    /// Called on product load and after entitlement refreshes — the App
    /// Store flips eligibility off the moment the user redeems the trial.
    private func refreshTrialEligibility() async {
        guard let subscription = yearlyProduct?.subscription else {
            isEligibleForYearlyTrial = false
            return
        }
        isEligibleForYearlyTrial = await subscription.isEligibleForIntroOffer
    }

    #if DEBUG
    /// Toggle Pro for local testing. Compiled out of release builds entirely.
    func debugTogglePro() {
        isPro.toggle()
    }
    #endif

    // MARK: - Private

    private func purchase(productId: String) async -> Bool {
        guard let product = products.first(where: { $0.id == productId }) else { return false }
        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                let transaction = try checkVerified(verification)
                isPro = true
                await transaction.finish()
                return true
            case .userCancelled, .pending:
                return false
            @unknown default:
                return false
            }
        } catch {
            print("ProManager: purchase failed — \(error)")
            return false
        }
    }

    private func refreshEntitlements() async {
        var entitled = false
        for await result in Transaction.currentEntitlements {
            guard let transaction = try? checkVerified(result) else { continue }
            if Self.entitlingProductIds.contains(transaction.productID) {
                entitled = true
                break
            }
        }
        isPro = entitled
        // Trial eligibility flips off once a user redeems the offer; refresh
        // so the paywall stops advertising it after a successful purchase.
        await refreshTrialEligibility()
    }

    private func listenForTransactions() -> Task<Void, Error> {
        Task.detached {
            for await result in Transaction.updates {
                guard case .verified(let transaction) = result else { continue }
                if Self.entitlingProductIds.contains(transaction.productID) {
                    await MainActor.run { [weak self] in
                        self?.isPro = true
                    }
                }
                await transaction.finish()
            }
        }
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified: throw StoreError.verificationFailed
        case .verified(let value): return value
        }
    }

    enum StoreError: Error {
        case verificationFailed
    }
}

extension ProManager {
    /// Free-tier limits applied by the UI layer. Mirror of Android's companion
    /// constants in `ProManager.kt`.
    enum FreeTier {
        static let activeProgramSlots = 1
        static let customRoutineLimit = 3
        static let historyDays = 30
        static let insightPreviewCount = 1
    }
}

// MARK: - HealthKit (iOS sibling of Android HealthConnectService)

/// Whether HealthKit can be reached. On iOS this is essentially always
/// available on iPhone; iPad sometimes reports unavailable.
@MainActor
final class HealthKitService: ObservableObject {
    /// User-facing toggle persisted in UserDefaults — survives process death.
    @Published private(set) var isEnabled: Bool {
        didSet { UserDefaults.standard.set(isEnabled, forKey: Self.enabledKey) }
    }

    /// Whether the user has granted write access for `HKWorkoutType`. Refreshed
    /// on demand; HealthKit doesn't push updates when the user revokes outside
    /// the app, so the Settings screen calls `refresh()` whenever it appears.
    @Published private(set) var isAuthorized: Bool = false

    /// True on devices where HealthKit is supported (essentially all iPhones).
    let isAvailable: Bool = HKHealthStore.isHealthDataAvailable()

    private let store = HKHealthStore()
    private let workoutType: HKSampleType = HKObjectType.workoutType()

    private static let enabledKey = "fittrack_healthkit_enabled"

    init() {
        self.isEnabled = UserDefaults.standard.bool(forKey: Self.enabledKey)
        Task { await refresh() }
    }

    /// Re-query the OS for write authorization status; safe to call repeatedly.
    /// If the user revoked permission outside our app, automatically flip the
    /// toggle off so we don't silently fail every workout.
    func refresh() async {
        guard isAvailable else {
            isAuthorized = false
            return
        }
        let status = store.authorizationStatus(for: workoutType)
        isAuthorized = (status == .sharingAuthorized)
        if !isAuthorized && isEnabled {
            isEnabled = false
        }
    }

    func requestAuthorization() async {
        guard isAvailable else { return }
        do {
            try await store.requestAuthorization(toShare: [workoutType], read: [])
            await refresh()
            // If the user just granted, flip enabled on as a convenience —
            // matches the Android Health Connect flow.
            if isAuthorized {
                isEnabled = true
            }
        } catch {
            print("HealthKitService: auth failed — \(error)")
        }
    }

    func setEnabled(_ value: Bool) {
        isEnabled = value
    }

    /// Write a finished strength-training session to Apple Health.
    /// No-ops cleanly if disabled / not authorized / unavailable — callers
    /// don't need to pre-check.
    func writeWorkout(startDate: Date, endDate: Date) async {
        guard isEnabled, isAuthorized, isAvailable else { return }
        guard endDate > startDate else { return }

        let configuration = HKWorkoutConfiguration()
        configuration.activityType = .traditionalStrengthTraining

        let builder = HKWorkoutBuilder(
            healthStore: store,
            configuration: configuration,
            device: .local()
        )

        do {
            try await builder.beginCollection(at: startDate)
            try await builder.endCollection(at: endDate)
            _ = try await builder.finishWorkout()
        } catch {
            print("HealthKitService: write failed — \(error)")
        }
    }
}

// MARK: - Subscription period formatting

/// Render a StoreKit 2 `Product.SubscriptionPeriod` (e.g. 7 days, 1 month) as
/// a paywall-friendly string. Mirrors the Android `formatBillingPeriod` helper
/// so trial copy matches across platforms.
func formatPeriod(_ period: Product.SubscriptionPeriod) -> String {
    let value = period.value
    switch period.unit {
    case .day:
        return value == 1 ? "1 day" : "\(value) days"
    case .week:
        let days = value * 7
        return days == 1 ? "1 day" : "\(days) days"
    case .month:
        return value == 1 ? "1 month" : "\(value) months"
    case .year:
        return value == 1 ? "1 year" : "\(value) years"
    @unknown default:
        return "\(value)"
    }
}
