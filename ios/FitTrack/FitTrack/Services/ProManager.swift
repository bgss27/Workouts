import Foundation
import StoreKit

@MainActor
class ProManager: ObservableObject {
    @Published var isPro: Bool {
        didSet { UserDefaults.standard.set(isPro, forKey: "fittrack_is_pro") }
    }

    static let monthlyProductId = "fittrack_pro_monthly"
    static let yearlyProductId = "fittrack_pro_yearly"

    private var products: [Product] = []
    private var updateListenerTask: Task<Void, Error>?

    init() {
        self.isPro = UserDefaults.standard.bool(forKey: "fittrack_is_pro")
        updateListenerTask = listenForTransactions()
        Task { await loadProducts() }
        Task { await checkExistingPurchases() }
    }

    deinit { updateListenerTask?.cancel() }

    func loadProducts() async {
        do {
            products = try await Product.products(for: [Self.monthlyProductId, Self.yearlyProductId])
        } catch {
            print("Failed to load products: \(error)")
        }
    }

    var monthlyProduct: Product? { products.first { $0.id == Self.monthlyProductId } }
    var yearlyProduct: Product? { products.first { $0.id == Self.yearlyProductId } }

    var monthlyPrice: String { monthlyProduct?.displayPrice ?? "$4.99/month" }
    var yearlyPrice: String { yearlyProduct?.displayPrice ?? "$29.99/year" }

    func purchase(_ product: Product) async throws -> Bool {
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
    }

    func purchaseMonthly() async -> Bool {
        guard let product = monthlyProduct else { return false }
        return (try? await purchase(product)) ?? false
    }

    func purchaseYearly() async -> Bool {
        guard let product = yearlyProduct else { return false }
        return (try? await purchase(product)) ?? false
    }

    func restorePurchases() async {
        await checkExistingPurchases()
    }

    func debugTogglePro() {
        isPro.toggle()
    }

    func canAccessProgram(daysPerWeek: Int) -> Bool {
        isPro || daysPerWeek <= 3
    }

    // MARK: - Private

    private func checkExistingPurchases() async {
        for await result in Transaction.currentEntitlements {
            if let transaction = try? checkVerified(result) {
                if transaction.productID == Self.monthlyProductId || transaction.productID == Self.yearlyProductId {
                    isPro = true
                    return
                }
            }
        }
    }

    private func listenForTransactions() -> Task<Void, Error> {
        Task.detached { [weak self] in
            for await result in Transaction.updates {
                if let transaction = try? self?.checkVerified(result) {
                    await MainActor.run { self?.isPro = true }
                    await transaction.finish()
                }
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
