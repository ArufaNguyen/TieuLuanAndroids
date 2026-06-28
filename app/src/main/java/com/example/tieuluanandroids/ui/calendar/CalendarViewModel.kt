package com.example.tieuluanandroids.ui.calendar

import androidx.lifecycle.ViewModel
import com.example.tieuluanandroids.data.model.Event
import com.example.tieuluanandroids.data.repository.SmartCalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow

data class CalendarUiState(
    val isLoading: Boolean = false,
    val events: List<Event> = emptyList(),
    )

class CalendarViewModel(
    private val repository: SmartCalendarRepository
) : ViewModel(){
    private val _uiState = MutableStateFlow(CalendarUiState())

}