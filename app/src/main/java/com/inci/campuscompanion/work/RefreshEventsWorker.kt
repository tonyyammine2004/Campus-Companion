package com.inci.campuscompanion.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inci.campuscompanion.data.repository.EventsRepository
import java.io.IOException

class RefreshEventsWorker(
    appContext: Context,
    params: WorkerParameters,
    private val repository: EventsRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            repository.refreshEvents()
            Result.success()
        } catch (error: IOException) {
            Result.retry()
        }
    }
}