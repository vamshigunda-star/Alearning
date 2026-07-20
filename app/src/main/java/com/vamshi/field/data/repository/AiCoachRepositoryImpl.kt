package com.vamshi.field.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.vamshi.field.domain.repository.AiCoachRepository
import com.vamshi.field.domain.repository.AiCoachStatus
import com.google.ai.edge.aicore.GenerationConfig
import com.google.ai.edge.aicore.GenerativeModel
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

    private var generativeModel: GenerativeModel? = null

    override suspend fun checkAvailability() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _status.value = AiCoachStatus.UNSUPPORTED
            return
        }
        try {
            val builder = GenerationConfig.Builder()
            builder.context = context
            val config = builder.build()
            
            // Re-instantiate GenerativeModel correctly using the Android AICore package.
            generativeModel = GenerativeModel(config)
            _status.value = AiCoachStatus.READY
        } catch (e: Exception) {
            Log.e("AiCoachRepository", "AICore not available or model not downloaded", e)
            _status.value = AiCoachStatus.ERROR
        }
    }

    override suspend fun sendMessage(prompt: String, contextData: String?): String {
        if (_status.value != AiCoachStatus.READY) {
            return "AI Coach is currently unavailable on this device."
        }
        val model = generativeModel ?: return "AI Coach is currently unavailable on this device."

        return try {
            val systemInstruction = "You are an expert athletic development coach. Focus on providing actionable training plans and interpreting athlete test results."
            val fullPrompt = if (contextData != null) {
                "System: $systemInstruction\n\nAthlete Data:\n$contextData\n\nUser Question:\n$prompt"
            } else {
                "System: $systemInstruction\n\nUser Question:\n$prompt"
            }
            val result = model.generateContent(fullPrompt)
            result.text ?: "No response from AI Coach."
        } catch (e: Exception) {
            Log.e("AiCoachRepository", "Error generating content", e)
            "Sorry, there was an error processing your request."
        }
    }
}
