package com.example.tieuluanandroids.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tieuluanandroids.data.model.SessionInfo
import com.example.tieuluanandroids.data.repository.SmartCalendarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SessionUiState(
    val session: SessionInfo? = null
)

class SessionViewModel(repository: SmartCalendarRepository) : ViewModel() {
    val uiState: StateFlow<SessionUiState> = repository.observeSession()
        .map(::SessionUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionUiState()
        )
}
