package com.fittrack.app.domain.ml

import kotlin.math.abs
import kotlin.math.sqrt

object StatisticalUtils {

    /** Calculate the mean of a list of values */
    fun mean(values: List<Double>): Double =
        if (values.isEmpty()) 0.0 else values.average()

    /** Calculate standard deviation */
    fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }

    /** Coefficient of variation (CV) = std / mean, measures relative variability */
    fun coefficientOfVariation(values: List<Double>): Double {
        val mean = mean(values)
        if (mean == 0.0) return 0.0
        return standardDeviation(values) / abs(mean)
    }

    /** Calculate the moving average with a given window size */
    fun movingAverage(values: List<Double>, windowSize: Int): List<Double> {
        if (values.size < windowSize) return values
        return values.windowed(windowSize) { it.average() }
    }

    /** Detect if recent values are in a plateau (low variance in last N values) */
    fun isPlateaued(values: List<Double>, windowSize: Int = 4, cvThreshold: Double = 0.03): Boolean {
        if (values.size < windowSize) return false
        val recent = values.takeLast(windowSize)
        return coefficientOfVariation(recent) < cvThreshold
    }

    /** Calculate the percentage change between two values */
    fun percentChange(from: Double, to: Double): Double {
        if (from == 0.0) return 0.0
        return ((to - from) / from) * 100.0
    }

    /** Calculate exponentially weighted moving average (EWMA) */
    fun ewma(values: List<Double>, alpha: Double = 0.3): List<Double> {
        if (values.isEmpty()) return emptyList()
        val result = mutableListOf(values.first())
        for (i in 1 until values.size) {
            result.add(alpha * values[i] + (1 - alpha) * result.last())
        }
        return result
    }

    /** Find the index where a plateau begins (last N values with low variance) */
    fun plateauStartIndex(values: List<Double>, windowSize: Int = 4, cvThreshold: Double = 0.03): Int? {
        if (values.size < windowSize) return null
        for (i in values.size - windowSize downTo 0) {
            val window = values.subList(i, i + windowSize)
            if (coefficientOfVariation(window) >= cvThreshold) {
                return i + 1
            }
        }
        return 0
    }

    /** Calculate z-score for a value relative to the distribution */
    fun zScore(value: Double, mean: Double, stdDev: Double): Double {
        if (stdDev == 0.0) return 0.0
        return (value - mean) / stdDev
    }

    /** Simple percentile calculation */
    fun percentile(values: List<Double>, p: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val index = (p / 100.0 * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }
}
