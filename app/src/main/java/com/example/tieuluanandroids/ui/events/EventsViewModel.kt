package com.example.tieuluanandroids.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tieuluanandroids.data.model.AppResult
import com.example.tieuluanandroids.data.model.Event
import com.example.tieuluanandroids.data.repository.SmartCalendarRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventsUiState(
    val isLoading: Boolean = false,
    val events: List<Event> = emptyList(),
    val message: String? = null,
    val showOwner: Boolean = false
)

class EventsViewModel(
    private val repository: SmartCalendarRepository
) : ViewModel() {
    private var isSyncing = false
    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private val _errors = Channel<String>(Channel.BUFFERED)
    val errors = _errors.receiveAsFlow()

    init {
        observeLocalEvents()
        refresh()
    }

    private fun observeLocalEvents() {
        viewModelScope.launch {
            repository.observeEvents().collect { events ->
                showEvents(events)
            }
        }
    }

    private fun showEvents(events: List<Event>) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                events = events,
                message = null,
                showOwner = repository.isDevMode
            )
        }
    }

    fun refresh() {
        if (isSyncing) {
            return
        }

        viewModelScope.launch {
            isSyncing = true
            setLoading(true)
            try {
                handleSyncResult(repository.syncNow())
            } finally {
                isSyncing = false
            }
        }
    }

    private suspend fun handleSyncResult(result: AppResult<Unit>) {
        when (result) {
            is AppResult.Success -> setLoading(false)
            is AppResult.Error -> showSyncError(result.message)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isLoading = isLoading, message = null)
        }
    }

    private suspend fun showSyncError(message: String) {
        _uiState.update { currentState ->
            currentState.copy(isLoading = false, message = message)
        }
        _errors.send(message)
    }
}
