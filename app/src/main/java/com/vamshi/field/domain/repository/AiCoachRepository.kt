package com.example.alearning.domain.repository

import kotlinx.coroutines.flow.StateFlow

enum class AiCoachStatus {
    READY,
    UNSUPPORTED,
    DOWNLOADING,
    ERROR
}

interface AiCoachRepository {
    val status: StateFlow<AiCoachStatus>
    suspend fun checkAvailability()
    suspend fun sendMessage(prompt: String, contextData: String? = null): String
}

