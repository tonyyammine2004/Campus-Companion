package com.inci.campuscompanion.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.inci.campuscompanion.data.repository.CampusEvent
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.work.EventsRefreshScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface EventsUiState {
    object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    object Empty : EventsUiState
    data class Error(val message: String, val isRetryable: Boolean) : EventsUiState
}

class EventsViewModel(
    private val repository: EventsRepository,
    private val scheduler: EventsRefreshScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState

    init {
        fetchEvents()
    }

    fun fetchEvents() {
        viewModelScope.launch {
            _uiState.value = EventsUiState.Loading
            try {
                val events = repository.getEvents()
                _uiState.value = if (events.isEmpty()) {
                    EventsUiState.Empty
                } else {
                    EventsUiState.Content(events)
                }
            } catch (_: IOException) {
                _uiState.value = EventsUiState.Error("Network error. Check connection.", isRetryable = true)
            } catch (e: HttpException) {
                _uiState.value = EventsUiState.Error("Server error: ${e.code()}", isRetryable = true)
            } catch (_: Exception) {
                _uiState.value = EventsUiState.Error("Safe generic error.", isRetryable = false)
            }
        }
    }

    sealed interface RefreshUi {
        data object Idle : RefreshUi
        data object Queued : RefreshUi
        data object Refreshing : RefreshUi
        data object Current : RefreshUi
        data object Failed : RefreshUi
    }

    val refreshState: StateFlow<RefreshUi> = (scheduler?.observeRefresh() ?: emptyFlow())
        .map { infos ->
            when (infos.firstOrNull()?.state) {
                WorkInfo.State.ENQUEUED -> RefreshUi.Queued
                WorkInfo.State.RUNNING -> RefreshUi.Refreshing
                WorkInfo.State.SUCCEEDED -> RefreshUi.Current
                WorkInfo.State.FAILED -> RefreshUi.Failed
                else -> RefreshUi.Idle
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RefreshUi.Idle
        )

    fun onRefreshClicked() {
        scheduler?.enqueueRefresh()
    }

    companion object {
        fun Factory(
            eventsRepository: EventsRepository,
            scheduler: EventsRefreshScheduler? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EventsViewModel(eventsRepository, scheduler) as T
                }
            }
    }
}
