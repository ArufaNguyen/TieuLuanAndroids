package com.example.tieuluanandroids.ui.addhar.discoveryjob

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tieuluanandroids.data.model.AppResult
import com.example.tieuluanandroids.data.model.DiscoveryJob
import com.example.tieuluanandroids.data.model.Event
import com.example.tieuluanandroids.data.repository.SmartCalendarRepository
import com.example.tieuluanandroids.ui.events.EventsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryJobUiState(
    val isLoading: Boolean = false,
    val jobs: List<DiscoveryJob> = emptyList(),
    val message: String? = null,
    val showOwner: Boolean = false
)

class DiscoveryJobViewModel(
    private val repository: SmartCalendarRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoveryJobUiState())
    val uiState: StateFlow<DiscoveryJobUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, message = null)
            }

            when (val result = repository.getDiscoveryJobs()) {
                is AppResult.Success -> {
                    _uiState.value = DiscoveryJobUiState(
                        jobs = result.data
                    )
                }

                is AppResult.Error -> {
                    _uiState.value = DiscoveryJobUiState(
                        message = result.message
                    )
                }
            }
        }
    }

}
