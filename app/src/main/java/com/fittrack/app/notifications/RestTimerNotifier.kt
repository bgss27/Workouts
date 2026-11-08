package com.fittrack.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fittrack.app.MainActivity
import com.fittrack.app.R

/**
 * Keeps the rest timer visible and audible when the phone is locked.
 *
 * While a rest runs, a silent ongoing notification counts down to the end
 * time. An [AlarmManager] broadcast fires at the end and [RestAlarmReceiver]
 * swaps it for an audible "Rest over" notification. Both are removed when the
 * timer is cancelled, restarted, or the workout ends.
 */
object RestTimerNotifier {
    private const val TAG = "RestTimerNotifier"
    const val CHANNEL_ALERT = "rest_timer"
    const val CHANNEL_PROGRESS = "rest_timer_progress"
    const val NOTIFICATION_ID = 4101
    const val EXTRA_EXERCISE = "exercise"
    private const val ALARM_REQUEST = 4102

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ALERT) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERT, "Rest timer alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Sounds when a rest period ends"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_PROGRESS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_PROGRESS, "Rest timer countdown", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows the running rest countdown"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Post the countdown and arm the end-of-rest alarm. No-op without permission. */
    fun schedule(context: Context, endsAtMillis: Long, exerciseName: String?) {
        cancel(context)
        if (!hasPermission(context)) return
        ensureChannels(context)

        val countdown = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle("Resting")
            .setContentText(exerciseName?.let { "Next: $it" } ?: "Rest timer running")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endsAtMillis)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openAppIntent(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, countdown) }

        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = alarmIntent(context, exerciseName)
        val exact = canScheduleExact(context)
        runCatching {
            // The alarm is a backstop for when the process has been killed
            // mid-rest; while the app is alive, the in-app timer posts the
            // alert itself at the exact second. Exact alarms need a grant on
            // Android 12+ (see [exactAlarmSettingsIntent]); without it the
            // system may deliver the inexact one up to a minute late.
            if (exact) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, pi)
            else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, pi)
            Log.d(TAG, "rest alarm armed exact=$exact at $endsAtMillis")
        }.onFailure { Log.w(TAG, "rest alarm not armed", it) }
    }

    /** Whether the system lets this app schedule exact alarms. */
    fun canScheduleExact(context: Context): Boolean {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return false
        return Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()
    }

    /** System screen where the user can grant exact alarms (Android 12+). */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < 31) return null
        return Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(android.net.Uri.parse("package:" + context.packageName))
    }

    /** Disarm only the alarm, keeping any notification that is showing. */
    fun cancelAlarm(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(context, null))
    }

    /** Remove the countdown and disarm the alarm. */
    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(context, null))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** Called by [RestAlarmReceiver]: replace the countdown with the audible alert. */
    fun showRestOver(context: Context, exerciseName: String?) {
        if (!hasPermission(context)) return
        ensureChannels(context)
        val alert = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle("Rest over")
            .setContentText(exerciseName?.let { "Back to $it" } ?: "Next set")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(openAppIntent(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, alert) }
    }

    private fun alarmIntent(context: Context, exerciseName: String?): PendingIntent {
        val intent = Intent(context, RestAlarmReceiver::class.java).apply {
            action = RestAlarmReceiver.ACTION
            putExtra(EXTRA_EXERCISE, exerciseName)
        }
        return PendingIntent.getBroadcast(
            context, ALARM_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
