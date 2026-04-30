package com.example.alearning.ui.testing.stopwatch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.TimingMode
import com.example.alearning.domain.model.testing.CaptureMethod
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.testing.RecordTestResultUseCase
import com.example.alearning.domain.usecase.testing.StopwatchSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StopwatchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stopwatchSessionUseCase: StopwatchSessionUseCase,
    private val recordResult: RecordTestResultUseCase,
    private val testingRepository: TestingRepository
) : ViewModel() {

    private val eventId: String = savedStateHandle["eventId"] ?: ""
    private val fitnessTestId: String = savedStateHandle["fitnessTestId"] ?: ""
    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private val initialAthleteId: String? = savedStateHandle["athleteId"] // New: handle passed athleteId

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var confirmationJob: Job? = null
    private var startNanos: Long = 0L

    // Full list of unique athletes for this test session
    private var athletes: List<Individual> = emptyList()
    
    // Tracks completed trial counts per athlete for this specific session
    private var completionState = mutableMapOf<String, Int>()

    // For GROUP_START mode
    private var heats: List<List<Individual>> = emptyList()
    private var currentHeatIndex: Int = 0

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            try {
                val session = stopwatchSessionUseCase(eventId, fitnessTestId, groupId)
                val test = session.fitnessTest

                athletes = session.athletes
                completionState = session.trialCounts.toMutableMap()
                heats = session.heats

                val totalTrials = athletes.size * session.trialsPerAthlete
                val completedTrials = completionState.values.sum()

                val event = testingRepository.getEventById(eventId)

                when (test.timingMode) {
                    TimingMode.INDIVIDUAL -> {
                        _uiState.update {
                            it.copy(
                                mode = TimingMode.INDIVIDUAL,
                                eventName = event?.name ?: "",
                                testName = test.name,
                                testUnit = test.unit,
                                completedCount = completedTrials,
                                totalCount = totalTrials,
                                isLoading = false,
                                selectedAthleteId = initialAthleteId // Use passed ID instead of first entry
                            )
                        }
                    }
                    TimingMode.GROUP_START -> {
                        currentHeatIndex = findFirstIncompleteHeat(session)
                        _uiState.update {
                            it.copy(
                                mode = TimingMode.GROUP_START,
                                eventName = event?.name ?: "",
                                testName = test.name,
                                testUnit = test.unit,
                                heatAthletes = buildHeatAthletesList(session.trialsPerAthlete),
                                currentHeatNumber = currentHeatIndex + 1,
                                totalHeats = heats.size,
                                completedCount = completedTrials,
                                totalCount = totalTrials,
                                isLoading = false
                            )
                        }
                    }
                    else -> _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message, isLoading = false)
                }
            }
        }
    }

    private fun findFirstIncompleteHeat(session: com.example.alearning.domain.usecase.testing.StopwatchSession): Int {
        for (i in heats.indices) {
            val hasIncomplete = heats[i].any { (completionState[it.id] ?: 0) < session.trialsPerAthlete }
            if (hasIncomplete) return i
        }
        return 0
    }

    private fun buildHeatAthletesList(totalTrials: Int): List<AthleteQueueItem> {
        if (currentHeatIndex >= heats.size) return emptyList()
        return heats[currentHeatIndex].map { athlete ->
            val done = completionState[athlete.id] ?: 0
            AthleteQueueItem(
                athleteId = athlete.id,
                name = athlete.fullName,
                currentTrial = done + 1,
                totalTrials = totalTrials,
                status = if (done < totalTrials) AthleteStatus.WAITING else AthleteStatus.COMPLETED
            )
        }
    }

    fun onAction(action: StopwatchAction) {
        when (action) {
            is StopwatchAction.OnStartStop -> handleStartStop()
            is StopwatchAction.OnFinishTap -> handleFinishTap()
            is StopwatchAction.OnStopHeat -> handleStopHeat()
            is StopwatchAction.OnUndo -> handleUndo()
            is StopwatchAction.OnNext -> handleNext()
            is StopwatchAction.OnSelectAthlete -> {
                val currentPhase = _uiState.value.stopwatchPhase
                val currentMode = _uiState.value.mode
                
                if (currentMode == TimingMode.INDIVIDUAL) {
                    if (currentPhase != StopwatchPhase.RUNNING) {
                        _uiState.update { it.copy(
                            selectedAthleteId = action.athleteId,
                            stopwatchPhase = StopwatchPhase.READY,
                            elapsedMs = 0L,
                            confirmationData = null,
                            canUndo = false
                        ) }
                    }
                } else {
                    // Group Mode: allow selection to target finishers
                    _uiState.update { it.copy(selectedAthleteId = action.athleteId) }
                }
            }
            is StopwatchAction.OnResetHeat -> handleResetHeat()
            is StopwatchAction.OnResetAthlete -> handleResetAthlete()
            is StopwatchAction.OnSubmitPending -> submitPending()
            is StopwatchAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            is StopwatchAction.OnNavigateBack -> Unit 
        }
    }

    private fun handleStartStop() {
        val phase = _uiState.value.stopwatchPhase
        if (phase == StopwatchPhase.READY) {
            startTimer()
        } else if (phase == StopwatchPhase.RUNNING && _uiState.value.mode == TimingMode.INDIVIDUAL) {
            stopAndStageIndividual()
        }
    }

    private fun startTimer() {
        startNanos = System.nanoTime()
        _uiState.update { it.copy(stopwatchPhase = StopwatchPhase.RUNNING, elapsedMs = 0L) }

        if (_uiState.value.mode == TimingMode.GROUP_START) {
            _uiState.update { state ->
                state.copy(
                    heatAthletes = state.heatAthletes.map { 
                        if (it.status == AthleteStatus.WAITING) it.copy(status = AthleteStatus.ACTIVE) else it 
                    }
                )
            }
        }

        tickerJob = viewModelScope.launch {
            while (true) {
                val elapsed = (System.nanoTime() - startNanos) / 1_000_000
                _uiState.update { it.copy(elapsedMs = elapsed) }
                delay(10)
            }
        }
    }

    private fun stopAndStageIndividual() {
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        tickerJob?.cancel()

        val selectedId = _uiState.value.selectedAthleteId ?: return
        val athlete = athletes.find { it.id == selectedId } ?: return
        val currentTrial = (completionState[selectedId] ?: 0) + 1

        _uiState.update {
            it.copy(
                stopwatchPhase = StopwatchPhase.CONFIRMING,
                elapsedMs = elapsedMs,
                confirmationData = ConfirmationData(
                    athleteName = athlete.fullName,
                    timeMs = elapsedMs,
                    trialNumber = currentTrial
                ),
                canUndo = true,
                pendingResults = it.pendingResults + (selectedId to elapsedMs / 1000.0)
            )
        }
    }

    private fun handleFinishTap() {
        if (_uiState.value.mode != TimingMode.GROUP_START || _uiState.value.stopwatchPhase != StopwatchPhase.RUNNING) return

        val state = _uiState.value
        val targetedAthlete = if (state.selectedAthleteId != null) {
            state.heatAthletes.find { it.athleteId == state.selectedAthleteId && it.status == AthleteStatus.ACTIVE }
        } else {
            state.heatAthletes.find { it.status == AthleteStatus.ACTIVE }
        }

        if (targetedAthlete == null) return

        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        
        _uiState.update { s ->
            s.copy(
                selectedAthleteId = null,
                heatAthletes = s.heatAthletes.map { 
                    if (it.athleteId == targetedAthlete.athleteId) it.copy(status = AthleteStatus.CAPTURED, capturedTimeMs = elapsedMs) else it
                },
                pendingResults = s.pendingResults + (targetedAthlete.athleteId to elapsedMs / 1000.0)
            )
        }

        if (_uiState.value.heatAthletes.count { it.status == AthleteStatus.ACTIVE } == 0) {
            finishHeat(targetedAthlete.name, elapsedMs)
        }
    }

    private fun handleStopHeat() {
        tickerJob?.cancel()
        val lastCaptured = _uiState.value.heatAthletes.lastOrNull { it.status == AthleteStatus.CAPTURED }
        if (lastCaptured != null) {
            finishHeat(lastCaptured.name, lastCaptured.capturedTimeMs ?: 0L)
        } else {
            _uiState.update { it.copy(stopwatchPhase = StopwatchPhase.READY) }
        }
    }

    private fun handleResetHeat() {
        tickerJob?.cancel()
        val state = _uiState.value
        _uiState.update { s ->
            s.copy(
                stopwatchPhase = StopwatchPhase.READY,
                elapsedMs = 0L,
                selectedAthleteId = null,
                heatAthletes = s.heatAthletes.map { it.copy(status = AthleteStatus.WAITING, capturedTimeMs = null) },
                pendingResults = s.pendingResults.filterKeys { athleteId ->
                    s.heatAthletes.none { it.athleteId == athleteId }
                }
            )
        }
    }

    private fun handleResetAthlete() {
        tickerJob?.cancel()
        val currentAthleteId = _uiState.value.selectedAthleteId
        _uiState.update {
            it.copy(
                stopwatchPhase = StopwatchPhase.READY,
                elapsedMs = 0L,
                confirmationData = null,
                canUndo = false,
                pendingResults = if (currentAthleteId != null) it.pendingResults - currentAthleteId else it.pendingResults
            )
        }
    }

    private fun finishHeat(name: String, timeMs: Long) {
        tickerJob?.cancel()
        _uiState.update {
            it.copy(
                stopwatchPhase = StopwatchPhase.CONFIRMING,
                confirmationData = ConfirmationData(name, timeMs, 0),
                canUndo = false
            )
        }
    }

    private fun handleNext() {
        if (_uiState.value.mode == TimingMode.INDIVIDUAL) {
            val all = getAllAthletes()
            val nextAthlete = all.find { it.status == AthleteStatus.WAITING }
            
            _uiState.update { state ->
                state.copy(
                    stopwatchPhase = StopwatchPhase.READY,
                    selectedAthleteId = nextAthlete?.athleteId,
                    confirmationData = null,
                    canUndo = false,
                    completedCount = completionState.values.sum(),
                    elapsedMs = 0L,
                    isSessionComplete = nextAthlete == null && (completionState.values.sum() + state.pendingResults.size) >= state.totalCount
                )
            }
        } else {
            advanceToNextHeat()
        }
    }

    private fun advanceToNextHeat() {
        currentHeatIndex++
        if (currentHeatIndex >= heats.size) {
            _uiState.update { state -> state.copy(isSessionComplete = true) }
        } else {
            _uiState.update { state ->
                val trialsPerAthlete = if (athletes.isNotEmpty()) state.totalCount / athletes.size else 0
                state.copy(
                    stopwatchPhase = StopwatchPhase.READY,
                    elapsedMs = 0L,
                    heatAthletes = buildHeatAthletesList(trialsPerAthlete),
                    currentHeatNumber = currentHeatIndex + 1,
                    confirmationData = null,
                    completedCount = completionState.values.sum()
                )
            }
        }
    }

    private fun handleUndo() {
        val currentAthleteId = _uiState.value.selectedAthleteId ?: return
        _uiState.update { 
            it.copy(
                stopwatchPhase = StopwatchPhase.READY, 
                confirmationData = null, 
                canUndo = false,
                pendingResults = it.pendingResults - currentAthleteId
            ) 
        }
    }

    private fun submitPending() {
        val state = _uiState.value
        if (state.pendingResults.isEmpty() || state.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                for ((athleteId, rawScore) in state.pendingResults) {
                    val athlete = athletes.find { it.id == athleteId } ?: continue
                    val age = ((System.currentTimeMillis() - athlete.dateOfBirth) / 31_557_600_000L).toFloat()
                    recordResult(eventId, athleteId, fitnessTestId, rawScore, age, athlete.sex, CaptureMethod.STOPWATCH)
                    completionState[athleteId] = (completionState[athleteId] ?: 0) + 1
                }
                _uiState.update {
                    it.copy(
                        pendingResults = emptyMap(),
                        isSubmitting = false,
                        completedCount = completionState.values.sum()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Save failed: ${e.message}", isSubmitting = false) }
            }
        }
    }

    private suspend fun saveResult(athlete: Individual, timeMs: Long) {
        // Obsolete but kept for signature matching if needed elsewhere, 
        // though we now use submitPending
    }

    fun getAllAthletes(): List<AthleteQueueItem> {
        val count = athletes.size
        if (count == 0) return emptyList()
        val totalTrialsPerAthlete = _uiState.value.totalCount / count
        return athletes.map { athlete ->
            val done = completionState[athlete.id] ?: 0
            AthleteQueueItem(
                athleteId = athlete.id,
                name = athlete.fullName,
                currentTrial = done + 1,
                totalTrials = totalTrialsPerAthlete,
                status = if (done >= totalTrialsPerAthlete) AthleteStatus.COMPLETED else AthleteStatus.WAITING
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        confirmationJob?.cancel()
    }
}
