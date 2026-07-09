package com.example.alearning.data.repository

import android.content.Context
import android.os.Build
import com.example.alearning.domain.repository.AiCoachRepository
import com.example.alearning.domain.repository.AiCoachStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AiCoachRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AiCoachRepository {
    private val _status = MutableStateFlow(AiCoachStatus.DOWNLOADING)
    override val status: StateFlow<AiCoachStatus> = _status.asStateFlow()

    override suspend fun checkAvailability() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _status.value = AiCoachStatus.UNSUPPORTED
        } else {
            _status.value = AiCoachStatus.ERROR
        }
    }

    override suspend fun sendMessage(prompt: String, contextData: String?): String {
        return if (_status.value != AiCoachStatus.READY) {
            "AI Coach is currently unavailable on this device."
        } else {
            "Response to $prompt"
        }
    }
}
