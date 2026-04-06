package com.fittrack.app.domain.ml

/**
 * Simple linear regression: y = slope * x + intercept
 * Uses ordinary least squares (OLS) fitting.
 */
data class LinearRegressionResult(
    val slope: Double,
    val intercept: Double,
    val rSquared: Double, // Coefficient of determination (0-1, higher = better fit)
    val standardError: Double,
    val n: Int
) {
    /** Predict y for a given x */
    fun predict(x: Double): Double = slope * x + intercept

    /** Trend direction with confidence */
    val trend: TrendDirection
        get() = when {
            n < 3 -> TrendDirection.INSUFFICIENT_DATA
            rSquared < 0.1 -> TrendDirection.NO_CLEAR_TREND
            slope > 0 && rSquared >= 0.3 -> TrendDirection.IMPROVING
            slope < 0 && rSquared >= 0.3 -> TrendDirection.DECLINING
            slope > 0 -> TrendDirection.SLIGHTLY_IMPROVING
            slope < 0 -> TrendDirection.SLIGHTLY_DECLINING
            else -> TrendDirection.FLAT
        }

    /** Weekly rate of change (if x is in weeks) */
    val weeklyChangeRate: Double get() = slope

    /** Percentage change per unit x */
    fun percentChangePerUnit(): Double {
        if (intercept == 0.0) return 0.0
        return (slope / intercept) * 100.0
    }
}

enum class TrendDirection(val label: String) {
    IMPROVING("Improving"),
    SLIGHTLY_IMPROVING("Slightly Improving"),
    FLAT("Flat"),
    SLIGHTLY_DECLINING("Slightly Declining"),
    DECLINING("Declining"),
    NO_CLEAR_TREND("No Clear Trend"),
    INSUFFICIENT_DATA("Insufficient Data")
}

object LinearRegression {

    /**
     * Fit a simple linear regression model to the given data points.
     * @param xs Independent variable values (e.g., session index or week number)
     * @param ys Dependent variable values (e.g., estimated 1RM, volume)
     */
    fun fit(xs: List<Double>, ys: List<Double>): LinearRegressionResult {
        require(xs.size == ys.size) { "xs and ys must have the same size" }
        val n = xs.size
        if (n < 2) {
            return LinearRegressionResult(0.0, ys.firstOrNull() ?: 0.0, 0.0, 0.0, n)
        }

        val meanX = xs.average()
        val meanY = ys.average()

        var ssXY = 0.0
        var ssXX = 0.0
        var ssYY = 0.0

        for (i in xs.indices) {
            val dx = xs[i] - meanX
            val dy = ys[i] - meanY
            ssXY += dx * dy
            ssXX += dx * dx
            ssYY += dy * dy
        }

        if (ssXX == 0.0) {
            return LinearRegressionResult(0.0, meanY, 0.0, 0.0, n)
        }

        val slope = ssXY / ssXX
        val intercept = meanY - slope * meanX

        // R-squared
        val ssRes = ys.indices.sumOf { i ->
            val predicted = slope * xs[i] + intercept
            val residual = ys[i] - predicted
            residual * residual
        }
        val rSquared = if (ssYY > 0) 1.0 - (ssRes / ssYY) else 0.0

        // Standard error of the estimate
        val standardError = if (n > 2) {
            Math.sqrt(ssRes / (n - 2))
        } else 0.0

        return LinearRegressionResult(
            slope = slope,
            intercept = intercept,
            rSquared = rSquared.coerceIn(0.0, 1.0),
            standardError = standardError,
            n = n
        )
    }

    /**
     * Fit using integer indices as x values (0, 1, 2, ...).
     */
    fun fitIndexed(ys: List<Double>): LinearRegressionResult {
        val xs = ys.indices.map { it.toDouble() }
        return fit(xs, ys)
    }
}
