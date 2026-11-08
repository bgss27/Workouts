package com.fittrack.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires when a rest period ends and posts the audible "Rest over" alert. */
class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        RestTimerNotifier.showRestOver(context, intent.getStringExtra(RestTimerNotifier.EXTRA_EXERCISE))
    }

    companion object {
        const val ACTION = "com.fittrack.app.REST_OVER"
    }
}
