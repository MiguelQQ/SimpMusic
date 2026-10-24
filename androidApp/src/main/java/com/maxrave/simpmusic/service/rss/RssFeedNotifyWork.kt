package com.maxrave.simpmusic.service.rss

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Blog RSS is disabled in Killmon. The class remains so a job queued by an older
 * install can complete without fetching or opening anything.
 */
class RssFeedNotifyWork(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}
