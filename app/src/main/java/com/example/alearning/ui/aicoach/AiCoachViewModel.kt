package com.example.alearning.ui.aicoach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.repository.AiCoachRepository
import com.example.alearning.domain.repository.AiCoachStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(val id: String, val text: String, val isFromUser: Boolean)

data class AiCoachUiState(
    val status: AiCoachStatus = AiCoachStatus.UNSUPPORTED,
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AiCoachAction {
    data class SendMessage(val prompt: String) : AiCoachAction
    object OnDismissError : AiCoachAction
}

@HiltViewModel
class AiCoachViewModel @Inject constructor(
    private val repository: AiCoachRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AiCoachUiState())
    val uiState: StateFlow<AiCoachUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.status.collect { status ->
                _uiState.update { it.copy(status = status) }
            }
        }
        viewModelScope.launch { repository.checkAvailability() }
    }

    fun onAction(action: AiCoachAction) {
        when (action) {
            is AiCoachAction.SendMessage -> sendMessage(action.prompt)
            AiCoachAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = ChatMessage(UUID.randomUUID().toString(), prompt, true)
        _uiState.update { it.copy(messages = it.messages + userMsg, isSending = true) }
        
        viewModelScope.launch {
            try {
                val response = repository.sendMessage(prompt)
                val aiMsg = ChatMessage(UUID.randomUUID().toString(), response, false)
                _uiState.update { it.copy(messages = it.messages + aiMsg, isSending = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isSending = false) }
            }
        }
    }
}
