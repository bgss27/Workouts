import Foundation

enum TrendDirection: String {
    case improving = "Improving"
    case slightlyImproving = "Slightly Improving"
    case flat = "Flat"
    case slightlyDeclining = "Slightly Declining"
    case declining = "Declining"
    case noClearTrend = "No Clear Trend"
    case insufficientData = "Insufficient Data"
}

struct LinearRegressionResult {
    let slope: Double
    let intercept: Double
    let rSquared: Double
    let n: Int

    func predict(_ x: Double) -> Double { slope * x + intercept }

    var trend: TrendDirection {
        if n < 3 { return .insufficientData }
        if rSquared < 0.1 { return .noClearTrend }
        if slope > 0 && rSquared >= 0.3 { return .improving }
        if slope < 0 && rSquared >= 0.3 { return .declining }
        if slope > 0 { return .slightlyImproving }
        if slope < 0 { return .slightlyDeclining }
        return .flat
    }
}

struct LinearRegression {
    static func fit(xs: [Double], ys: [Double]) -> LinearRegressionResult {
        let n = xs.count
        guard n >= 2, n == ys.count else {
            return LinearRegressionResult(slope: 0, intercept: ys.first ?? 0, rSquared: 0, n: n)
        }
        let meanX = xs.reduce(0, +) / Double(n)
        let meanY = ys.reduce(0, +) / Double(n)

        var ssXY = 0.0, ssXX = 0.0, ssYY = 0.0
        for i in 0..<n {
            let dx = xs[i] - meanX
            let dy = ys[i] - meanY
            ssXY += dx * dy
            ssXX += dx * dx
            ssYY += dy * dy
        }
        guard ssXX > 0 else {
            return LinearRegressionResult(slope: 0, intercept: meanY, rSquared: 0, n: n)
        }
        let slope = ssXY / ssXX
        let intercept = meanY - slope * meanX
        let ssRes = (0..<n).reduce(0.0) { acc, i in
            let r = ys[i] - (slope * xs[i] + intercept)
            return acc + r * r
        }
        let rSquared = ssYY > 0 ? max(0, min(1, 1 - ssRes / ssYY)) : 0

        return LinearRegressionResult(slope: slope, intercept: intercept, rSquared: rSquared, n: n)
    }

    static func fitIndexed(_ ys: [Double]) -> LinearRegressionResult {
        fit(xs: ys.indices.map { Double($0) }, ys: ys)
    }
}

struct StatisticalUtils {
    static func mean(_ values: [Double]) -> Double {
        values.isEmpty ? 0 : values.reduce(0, +) / Double(values.count)
    }

    static func coefficientOfVariation(_ values: [Double]) -> Double {
        guard values.count >= 2 else { return 0 }
        let m = mean(values)
        guard m != 0 else { return 0 }
        let variance = values.reduce(0.0) { $0 + ($1 - m) * ($1 - m) } / Double(values.count - 1)
        return sqrt(variance) / abs(m)
    }

    static func isPlateaued(_ values: [Double], window: Int = 4, threshold: Double = 0.03) -> Bool {
        guard values.count >= window else { return false }
        return coefficientOfVariation(Array(values.suffix(window))) < threshold
    }

    static func ewma(_ values: [Double], alpha: Double = 0.3) -> [Double] {
        guard !values.isEmpty else { return [] }
        var result = [values[0]]
        for i in 1..<values.count {
            result.append(alpha * values[i] + (1 - alpha) * result.last!)
        }
        return result
    }
}
