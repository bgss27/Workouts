package com.fittrack.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fittrack.app.FitTrackApp
import java.util.concurrent.TimeUnit

/**
 * Two triggers: a debounced one-off after local writes, and a 15-minute
 * periodic pass to pull other devices' changes. Both require network and
 * are no-ops when signed out.
 */
object SyncScheduler {
    private const val PERIODIC = "fittrack_sync_periodic"
    private const val ONE_OFF = "fittrack_sync_now"

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Push soon after a change; REPLACE collapses bursts into one run. */
    fun requestSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_OFF, ExistingWorkPolicy.REPLACE, request)
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? FitTrackApp ?: return Result.failure()
        if (!app.appModule.accountManager.isSignedIn) return Result.success()
        val error = app.appModule.syncEngine.syncNow()
        return if (error == null) Result.success() else Result.retry()
    }
}
