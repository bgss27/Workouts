package com.fittrack.app.data.health

import android.content.Context
import android.content.SharedPreferences
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId

/**
 * Whether Health Connect is reachable on this device.
 *
 * - [UNSUPPORTED] — running on an OS / device combo Health Connect doesn't
 *   support (e.g. SDK < 26). The Settings UI hides the option entirely.
 * - [NOT_INSTALLED] — supported, but the user hasn't installed the Health
 *   Connect provider. The Settings UI offers a "Get on Play Store" link.
 * - [AVAILABLE] — ready to request permissions and write records.
 */
enum class HealthConnectAvailability { UNSUPPORTED, NOT_INSTALLED, AVAILABLE }

/**
 * Step counts read from Health Connect. [daily] is the last seven calendar
 * days oldest-first, ending with today; [today] is the same as the last entry.
 */
data class StepsSummary(
    val today: Long,
    val daily: List<Long>,
) {
    val weeklyAverage: Long
        get() = if (daily.isEmpty()) 0 else daily.sum() / daily.size
}

/**
 * Wraps the Health Connect client for FitTrack's two needs:
 *
 *   1. The Settings screen needs to know if the user can connect, whether
 *      they've granted permissions, and whether sync is currently enabled.
 *   2. The active-workout finish flow needs a fire-and-forget write that
 *      no-ops cleanly when the user hasn't enabled / granted access.
 *
 * The service holds an applicationContext only — never an Activity — so it
 * is safe to keep alive for the lifetime of the process.
 */
class HealthConnectService(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val availability: HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(appContext)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.UNSUPPORTED
        }

    private val client: HealthConnectClient? =
        if (availability == HealthConnectAvailability.AVAILABLE) {
            runCatching { HealthConnectClient.getOrCreate(appContext) }.getOrNull()
        } else null

    /** Permissions for workout sync (write-only). */
    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    )

    /**
     * Permission for the Home step tracker. Kept separate from [permissions]
     * so a user can see steps without turning on workout sync, and vice versa.
     */
    val stepsPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
    )

    /**
     * The Activity Result contract a Composable can launch with these
     * [permissions] to bring up the Health Connect grant dialog.
     */
    fun permissionRequestContract() =
        PermissionController.createRequestPermissionResultContract()

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    private val _stepsGranted = MutableStateFlow(false)
    val stepsGranted: StateFlow<Boolean> = _stepsGranted

    /** Re-query the OS for granted permissions; safe to call repeatedly. */
    suspend fun refreshPermissions() {
        val granted = client?.permissionController?.getGrantedPermissions() ?: emptySet()
        _permissionsGranted.value = granted.containsAll(permissions)
        _stepsGranted.value = granted.containsAll(stepsPermissions)
        // If the user revoked permission outside our app, automatically flip
        // sync off so we don't silently fail every workout.
        if (!_permissionsGranted.value && _enabled.value) {
            setEnabled(false)
        }
    }

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    }

    /**
     * Write a finished strength-training session to Health Connect.
     * No-ops if Health Connect is unavailable, sync is disabled, or
     * permissions aren't granted — callers don't need to pre-check.
     */
    suspend fun writeWorkout(
        startTimeMillis: Long,
        endTimeMillis: Long,
        title: String? = null,
    ): Result<Unit> {
        if (!_enabled.value) return Result.success(Unit)
        val c = client ?: return Result.success(Unit)
        if (!_permissionsGranted.value) return Result.success(Unit)
        if (endTimeMillis <= startTimeMillis) return Result.success(Unit)

        return runCatching {
            val zone = ZoneId.systemDefault()
            val startInstant = Instant.ofEpochMilli(startTimeMillis)
            val endInstant = Instant.ofEpochMilli(endTimeMillis)
            val record = ExerciseSessionRecord(
                startTime = startInstant,
                startZoneOffset = zone.rules.getOffset(startInstant),
                endTime = endInstant,
                endZoneOffset = zone.rules.getOffset(endInstant),
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                title = title,
            )
            c.insertRecords(listOf(record))
            Unit
        }
    }

    /**
     * Today's step total plus the last seven days, aggregated by Health
     * Connect across every connected source (phone sensor, watch, etc.).
     * Returns null when the client is unavailable or permission is missing.
     */
    suspend fun readSteps(): StepsSummary? {
        val c = client ?: return null
        if (!_stepsGranted.value) return null
        return runCatching {
            val today = LocalDate.now()
            val weekStart = today.minusDays(6).atStartOfDay()
            val now = LocalDateTime.now()

            val grouped = c.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(weekStart, now),
                    timeRangeSlicer = Period.ofDays(1),
                )
            )
            val byDay = grouped.associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
            val daily = (6 downTo 0).map { byDay[today.minusDays(it.toLong())] ?: 0L }

            // Today's number straight from the aggregate so it matches the
            // system Health app even if the day bucket is still filling.
            val todayTotal = c.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(today.atTime(LocalTime.MIDNIGHT), now),
                )
            )[StepsRecord.COUNT_TOTAL] ?: 0L

            StepsSummary(today = todayTotal, daily = daily.dropLast(1) + todayTotal)
        }.getOrNull()
    }

    companion object {
        private const val PREFS = "fittrack_health"
        private const val KEY_ENABLED = "enabled"
    }
}
