package com.vaibhawmishra.voela.data.youtube

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Thin WorkManager seam: enqueue a single unique extraction job and observe it.
// Unique-by-name means the UI re-attaches to a running/finished job after the
// app (or process) restarts.
class ExtractionRepository(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    val workInfo: Flow<WorkInfo?> =
        workManager.getWorkInfosForUniqueWorkFlow(Extraction.WORK_NAME).map { it.firstOrNull() }

    fun start(url: String) {
        val request = OneTimeWorkRequestBuilder<AudioExtractionWorker>()
            .setInputData(workDataOf(Extraction.KEY_URL to url))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(Extraction.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() = workManager.cancelUniqueWork(Extraction.WORK_NAME)

    // Drop the finished job so a dismissed result doesn't re-attach after restart
    fun clearFinished() = workManager.pruneWork()
}
