package com.example.tieuluanandroids.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tieuluanandroids.data.repository.SmartCalendarRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class LoginUiState(val isLoading: Boolean = false)

sealed interface LoginEvent {
    data object MissingFields : LoginEvent
    data class ShowMessage(val message: String) : LoginEvent
    data object NavigateToEvents : LoginEvent
}

class LoginViewModel(
    private val repository: SmartCalendarRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _events.trySend(LoginEvent.MissingFields)
            return
        }
        if (_uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            val result = repository.login(username, password)
            _uiState.value = LoginUiState()
            _events.send(LoginEvent.ShowMessage(result.message))
            if (result.success) {
                _events.send(LoginEvent.NavigateToEvents)
            }
        }
    }

    fun continueInDevMode() {
        if (_uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            val result = repository.checkBackendDevMode()
            if (result.success) {
                repository.enableDevMode()
            }
            _uiState.value = LoginUiState()
            if (result.success) {
                _events.send(LoginEvent.NavigateToEvents)
            } else {
                _events.send(LoginEvent.ShowMessage(result.message))
            }
        }
    }
}
