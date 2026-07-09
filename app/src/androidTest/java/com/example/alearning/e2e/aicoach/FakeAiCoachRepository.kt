package com.example.alearning.e2e.aicoach

import com.example.alearning.domain.repository.AiCoachStatus
import com.example.alearning.domain.repository.AiCoachRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAiCoachRepository @Inject constructor() : AiCoachRepository {
    private val _status = MutableStateFlow(AiCoachStatus.UNSUPPORTED)
    override val status: StateFlow<AiCoachStatus> = _status

    var nextResponse: String? = "Mock AI Response"
    var networkDelayMs: Long = 0L

    override suspend fun sendMessage(prompt: String, contextData: String?): String {
        if (networkDelayMs > 0L) kotlinx.coroutines.delay(networkDelayMs)
        return nextResponse ?: "Default Mock Response"
    }

    override suspend fun checkAvailability() {}
    
    fun setStatus(newStatus: AiCoachStatus) {
        _status.value = newStatus
    }
}
