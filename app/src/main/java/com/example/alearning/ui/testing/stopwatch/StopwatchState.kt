package com.example.alearning.ui.testing.stopwatch

import com.example.alearning.domain.model.standards.TimingMode

data class StopwatchUiState(
    val mode: TimingMode = TimingMode.INDIVIDUAL,
    val eventName: String = "",
    val testName: String = "",
    val testUnit: String = "",
    val stopwatchPhase: StopwatchPhase = StopwatchPhase.READY,
    val elapsedMs: Long = 0L,
    val currentAthlete: AthleteQueueItem? = null,
    val heatAthletes: List<AthleteQueueItem> = emptyList(),
    val currentHeatNumber: Int = 1,
    val totalHeats: Int = 1,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val confirmationData: ConfirmationData? = null,
    val canUndo: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSessionComplete: Boolean = false,
    val selectedAthleteId: String? = null
)

enum class StopwatchPhase { READY, RUNNING, CONFIRMING }

enum class AthleteStatus { WAITING, ACTIVE, CAPTURED, COMPLETED }

data class AthleteQueueItem(
    val athleteId: String,
    val name: String,
    val currentTrial: Int,
    val totalTrials: Int,
    val status: AthleteStatus = AthleteStatus.WAITING,
    val capturedTimeMs: Long? = null
)

data class ConfirmationData(
    val athleteName: String,
    val timeMs: Long,
    val trialNumber: Int
)

sealed interface StopwatchAction {
    data object OnStartStop : StopwatchAction
    data object OnFinishTap : StopwatchAction
    data object OnStopHeat : StopwatchAction
    data object OnUndo : StopwatchAction
    data object OnDismissError : StopwatchAction
    data object OnNavigateBack : StopwatchAction
    data object OnNext : StopwatchAction
    data class OnSelectAthlete(val athleteId: String) : StopwatchAction
    data object OnResetHeat : StopwatchAction
    data object OnResetAthlete : StopwatchAction
}


