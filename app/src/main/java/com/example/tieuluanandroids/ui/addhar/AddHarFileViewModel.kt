package com.example.tieuluanandroids.ui.addhar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tieuluanandroids.data.repository.SmartCalendarRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class UploadStatus { IDLE, UPLOADING, SUCCESS, ERROR }

data class AddHarFileUiState(
    val status: UploadStatus = UploadStatus.IDLE
)

class AddHarFileViewModel(
    private val repository: SmartCalendarRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddHarFileUiState())
    val uiState: StateFlow<AddHarFileUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun upload(fileName: String, bytes: ByteArray) {
        if (_uiState.value.status == UploadStatus.UPLOADING) {
            return
        }

        viewModelScope.launch {
            _uiState.value = AddHarFileUiState(status = UploadStatus.UPLOADING)
            val result = repository.uploadHar(fileName, bytes)
            val finalStatus = if (result.success) UploadStatus.SUCCESS else UploadStatus.ERROR
            _uiState.value = AddHarFileUiState(status = finalStatus)
            _messages.send(result.message)
        }
    }

    fun reportReadError(message: String) {
        _uiState.value = AddHarFileUiState(status = UploadStatus.ERROR)
        _messages.trySend(message)
    }
}
