package com.inci.campuscompanion.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow
class EventsRefreshScheduler(private val workManager: WorkManager) {

    private fun buildRequest(): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        return OneTimeWorkRequestBuilder<RefreshEventsWorker>()
            .setConstraints(constraints)
            .build()
    }
    companion object {
        const val UNIQUE_NAME = "refresh-campus-events"
    }

    fun enqueueRefresh() {
        workManager.enqueueUniqueWork(
            UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            buildRequest()
        )
    }

    fun observeRefresh(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_NAME)
}