package com.example.parishsync.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that runs once every 2 days to scrape MiniHQ
 * (naccanada.org/imis_prod/MiniHQ) for divine service schedules, DSG guides, and announcements.
 *
 * If the schedule and DSG guide are unchanged, it skips the update.
 */
class MiniHqScrapeWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  companion object {
    private const val TAG = "MiniHqScrapeWorker"
    const val UNIQUE_PERIODIC_WORK_NAME = "com.example.parishsync.MiniHqPeriodicScrape"
    const val UNIQUE_ONE_TIME_WORK_NAME = "com.example.parishsync.MiniHqOneTimeScrape"
    const val WORK_TAG = "MiniHqSync"
    const val INTERVAL_DAYS = 2L

    /**
     * Schedules periodic background scraping once every 2 days.
     */
    fun schedulePeriodicScrape(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val periodicRequest = PeriodicWorkRequestBuilder<MiniHqScrapeWorker>(
        INTERVAL_DAYS,
        TimeUnit.DAYS
      )
        .setInitialDelay(INTERVAL_DAYS, TimeUnit.DAYS)
        .setConstraints(constraints)
        .addTag(WORK_TAG)
        .build()

      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UNIQUE_PERIODIC_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        periodicRequest
      )
      Log.i(TAG, "Enqueued periodic MiniHQ scrape worker (interval: $INTERVAL_DAYS days).")
    }

    /**
     * Triggers an immediate one-time sync check on demand.
     */
    fun triggerImmediateScrape(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val oneTimeRequest = OneTimeWorkRequestBuilder<MiniHqScrapeWorker>()
        .setConstraints(constraints)
        .addTag(WORK_TAG)
        .build()

      WorkManager.getInstance(context).enqueueUniqueWork(
        UNIQUE_ONE_TIME_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        oneTimeRequest
      )
      Log.i(TAG, "Enqueued immediate one-time MiniHQ scrape worker.")
    }
  }

  override suspend fun doWork(): Result {
    Log.i(TAG, "Executing background MiniHQ scrape check...")
    return runCatching {
      val outcome = MiniHqScraperService.performSync(applicationContext)
      Log.i(TAG, "MiniHQ Scraper Result: ${outcome.status} - ${outcome.message}")

      if (outcome.status == MiniHqScraperService.SyncStatus.UPDATED) {
        ParishNotificationHelper.sendMiniHqSyncNotification(
          context = applicationContext,
          title = "MiniHQ Schedules Updated",
          body = "New divine service schedules or DSG guides are available for staging."
        )
      }

      Result.success()
    }.getOrElse { error ->
      Log.e(TAG, "Error during MiniHQ scrape work: ${error.message}", error)
      Result.retry()
    }
  }
}
