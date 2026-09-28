package com.fittrack.app.data.health

import android.content.Context
import android.content.SharedPreferences
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
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

    /** Permissions FitTrack asks for. Currently write-only — no reads. */
    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
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

    /** Re-query the OS for granted permissions; safe to call repeatedly. */
    suspend fun refreshPermissions() {
        val granted = client?.permissionController?.getGrantedPermissions() ?: emptySet()
        _permissionsGranted.value = granted.containsAll(permissions)
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

    companion object {
        private const val PREFS = "fittrack_health"
        private const val KEY_ENABLED = "enabled"
    }
}
