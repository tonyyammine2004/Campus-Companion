package com.inci.campuscompanion.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.inci.campuscompanion.data.repository.EventsRepository
import retrofit2.Converter

class RefreshEventsWorkerFactory(
    private val repository: EventsRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return if (workerClassName == RefreshEventsWorker::class.java.name) {
            RefreshEventsWorker(appContext, workerParameters, repository)
        } else {
            null
        }
    }

    companion object {
        fun create(): Converter.Factory {
            return RefreshEventsWorkerFactory.create()
        }
    }
}