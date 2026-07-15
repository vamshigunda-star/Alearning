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
    
    private val absentAthletes = mutableSetOf<String>()

    // For GROUP_START mode
    private var heats: List<List<Individual>> = emptyList()
    private var currentHeatIndex: Int = 0
    private var totalTrialsPerAthlete: Int = 0

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
                totalTrialsPerAthlete = session.trialsPerAthlete

                val activeAthletes = athletes.count { !absentAthletes.contains(it.id) }
                val totalTrials = activeAthletes * totalTrialsPerAthlete
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
                                sessionLoaded = true,
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
                                isLoading = false,
                                sessionLoaded = true
                            )
                        }
                    }
                    else -> _uiState.update { it.copy(isLoading = false) }
                }

                // Collect actual database results to ensure 100% accurate trail counts and enable past trial editing
                viewModelScope.launch {
                    testingRepository.getEventResults(eventId).collect { results ->
                        val testResults = results.filter { it.testId == fitnessTestId }.sortedBy { it.createdAt }
                        val historyMap = testResults.groupBy { it.individualId }
                        
                        val newCompletionState = mutableMapOf<String, Int>()
                        historyMap.forEach { (id, res) -> newCompletionState[id] = res.size }
                        
                        completionState = newCompletionState
                        _uiState.update { it.copy(historicalResults = historyMap, completedCount = newCompletionState.values.sum()) }
                        refreshHeatAthletes()
                    }
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
        val pendingResults = _uiState.value.pendingResults
        val isRunning = _uiState.value.stopwatchPhase == StopwatchPhase.RUNNING
        return heats[currentHeatIndex].map { athlete ->
            val done = completionState[athlete.id] ?: 0
            val isAbsent = absentAthletes.contains(athlete.id)
            val pendingScore = pendingResults[athlete.id]
            val isPending = pendingScore != null
            val status = when {
                isAbsent -> AthleteStatus.ABSENT
                isPending -> AthleteStatus.CAPTURED
                done >= totalTrials -> AthleteStatus.COMPLETED
                else -> if (isRunning) AthleteStatus.ACTIVE else AthleteStatus.WAITING
            }
            AthleteQueueItem(
                athleteId = athlete.id,
                name = athlete.fullName,
                currentTrial = minOf(totalTrials, done + (if (isPending) 1 else 0) + 1),
                totalTrials = totalTrials,
                status = status,
                capturedTimeMs = if (isPending) (pendingScore!! * 1000).toLong() else null,
                historicalTrials = _uiState.value.historicalResults[athlete.id] ?: emptyList()
            )
        }
    }

    fun onAction(action: StopwatchAction) {
        when (action) {
            is StopwatchAction.OnStartStop -> handleStartStop()
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
                }
            }
            is StopwatchAction.OnResetAthlete -> handleResetAthlete()
            
            // New Mass Timing Actions
            is StopwatchAction.OnToggleAbsent -> handleToggleAbsent(action.athleteId)
            is StopwatchAction.OnCaptureTime -> handleCaptureTime(action.athleteId)
            is StopwatchAction.OnOpenEditDialog -> _uiState.update { it.copy(editingAthleteId = action.athleteId, editingResultId = action.resultId) }
            is StopwatchAction.OnSaveEditedTime -> handleSaveEditedTime(action.athleteId, action.newTimeMs, action.resultId)
            is StopwatchAction.OnClearEntry -> handleClearEntry(action.athleteId, action.resultId)
            is StopwatchAction.OnCloseEditDialog -> _uiState.update { it.copy(editingAthleteId = null, editingResultId = null) }
            is StopwatchAction.OnRequestSubmit -> handleRequestSubmit()
            is StopwatchAction.OnDismissMissingEntriesDialog -> _uiState.update { it.copy(showMissingEntriesDialog = false) }
            
            is StopwatchAction.OnSubmitPending -> submitPending()
            is StopwatchAction.OnDismissTrialMessage -> _uiState.update { it.copy(showTrialCompletedMessage = false) }
            is StopwatchAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            is StopwatchAction.OnRequestBack -> {
                if (_uiState.value.hasPendingChanges) {
                    _uiState.update { it.copy(showDiscardDialog = true) }
                }
            }
            is StopwatchAction.OnConfirmDiscard -> {
                tickerJob?.cancel()
                _uiState.update {
                    it.copy(
                        showDiscardDialog = false,
                        pendingResults = emptyMap(),
                        confirmationData = null,
                        canUndo = false,
                        stopwatchPhase = StopwatchPhase.READY,
                        elapsedMs = 0L,
                        heatAthletes = it.heatAthletes.map { a ->
                            if (a.status == AthleteStatus.CAPTURED) a.copy(status = AthleteStatus.WAITING, capturedTimeMs = null) else a
                        }
                    )
                }
            }
            is StopwatchAction.OnDismissDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
            }
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
            refreshHeatAthletes()
        }

        tickerJob = viewModelScope.launch {
            while (true) {
                val elapsed = (System.nanoTime() - startNanos) / 1_000_000
                _uiState.update { it.copy(elapsedMs = elapsed) }
                delay(10)
            }
        }
    }

    private fun refreshHeatAthletes() {
        if (_uiState.value.mode == TimingMode.GROUP_START) {
            val trialsPerAthlete = if (athletes.isNotEmpty()) _uiState.value.totalCount / athletes.size else 0
            _uiState.update { it.copy(heatAthletes = buildHeatAthletesList(trialsPerAthlete)) }
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
                elapsedMs = elapsedMs,
                pendingResults = it.pendingResults + (selectedId to elapsedMs / 1000.0)
            )
        }
        submitPending() // Auto-save automatically
    }

    private fun handleToggleAbsent(athleteId: String) {
        if (absentAthletes.contains(athleteId)) absentAthletes.remove(athleteId) else absentAthletes.add(athleteId)
        updateDerivedState()
        refreshHeatAthletes()
    }

    private fun updateDerivedState() {
        val activeAthletes = athletes.count { !absentAthletes.contains(it.id) }
        val newTotalCount = activeAthletes * totalTrialsPerAthlete
        val isComplete = _uiState.value.selectedAthleteId == null && 
                         _uiState.value.pendingResults.isEmpty() && 
                         activeAthletes > 0 && 
                         getMinimumCompletedTrials() >= totalTrialsPerAthlete
        
        _uiState.update { it.copy(
            totalCount = newTotalCount,
            isSessionComplete = isComplete
        )}
    }

    private fun handleCaptureTime(athleteId: String) {
        if (_uiState.value.stopwatchPhase != StopwatchPhase.RUNNING) return
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        
        _uiState.update { s ->
            s.copy(pendingResults = s.pendingResults + (athleteId to elapsedMs / 1000.0))
        }
        refreshHeatAthletes()
    }
    
    private fun handleSaveEditedTime(athleteId: String, newTimeMs: Long, resultId: String?) {
        if (resultId != null) {
            // Edit historical result
            viewModelScope.launch {
                try {
                    val athlete = athletes.find { it.id == athleteId } ?: return@launch
                    val age = ((System.currentTimeMillis() - athlete.dateOfBirth) / 31_557_600_000L).toFloat()
                    testingRepository.deleteResultById(resultId)
                    recordResult(eventId, athleteId, fitnessTestId, newTimeMs / 1000.0, age, athlete.sex, CaptureMethod.STOPWATCH)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Failed to edit time: ${e.message}") }
                }
            }
            _uiState.update { s -> s.copy(editingAthleteId = null, editingResultId = null) }
        } else {
            // Edit pending result
            _uiState.update { s ->
                s.copy(
                    pendingResults = s.pendingResults + (athleteId to newTimeMs / 1000.0),
                    editingAthleteId = null,
                    editingResultId = null
                )
            }
            refreshHeatAthletes()
        }
    }
    
    private fun handleClearEntry(athleteId: String, resultId: String?) {
        if (resultId != null) {
            // Delete historical result
            viewModelScope.launch {
                try {
                    testingRepository.deleteResultById(resultId)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Failed to clear time: ${e.message}") }
                }
            }
            _uiState.update { s -> s.copy(editingAthleteId = null, editingResultId = null) }
        } else {
            // Clear pending result
            _uiState.update { s ->
                s.copy(
                    pendingResults = s.pendingResults - athleteId,
                    editingAthleteId = null,
                    editingResultId = null
                )
            }
            refreshHeatAthletes()
        }
    }
    
    private fun handleRequestSubmit() {
        val hasMissing = _uiState.value.heatAthletes.any { it.status == AthleteStatus.ACTIVE || it.status == AthleteStatus.WAITING }
        if (hasMissing) {
            _uiState.update { it.copy(showMissingEntriesDialog = true) }
        } else {
            submitPending()
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



    private fun handleNext() {
        if (_uiState.value.mode == TimingMode.INDIVIDUAL) {
            val all = getAllAthletes()
            val currentId = _uiState.value.selectedAthleteId
            val currentIndex = all.indexOfFirst { it.athleteId == currentId }
            
            // Sequential search for the next waiting athlete
            val nextAthlete = if (currentIndex != -1) {
                all.drop(currentIndex + 1).find { it.status == AthleteStatus.WAITING }
                    ?: all.take(currentIndex).find { it.status == AthleteStatus.WAITING }
            } else {
                all.find { it.status == AthleteStatus.WAITING }
            }
            
            _uiState.update { state ->
                state.copy(
                    stopwatchPhase = StopwatchPhase.READY,
                    selectedAthleteId = nextAthlete?.athleteId,
                    confirmationData = null,
                    canUndo = false,
                    completedCount = completionState.values.sum(),
                    elapsedMs = 0L,
                    isSessionComplete = nextAthlete == null && completionState.values.sum() >= state.totalCount
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
                
                val shouldShowMessage = state.mode == TimingMode.INDIVIDUAL

                if (state.mode == TimingMode.GROUP_START) {
                    tickerJob?.cancel()
                    _uiState.update {
                        it.copy(
                            pendingResults = emptyMap(),
                            isSubmitting = false,
                            completedCount = completionState.values.sum(),
                            showMissingEntriesDialog = false
                        )
                    }
                    advanceToNextHeat()
                } else {
                    _uiState.update {
                        val totalCompleted = completionState.values.sum()
                        it.copy(
                            pendingResults = emptyMap(),
                            isSubmitting = false,
                            completedCount = totalCompleted,
                            showMissingEntriesDialog = false,
                            elapsedMs = 0L,
                            stopwatchPhase = StopwatchPhase.READY,
                            showTrialCompletedMessage = shouldShowMessage
                        )
                    }
                    updateDerivedState()
                    refreshHeatAthletes()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Save failed: ${e.message}", isSubmitting = false) }
            }
        }
    }

    private fun getMinimumCompletedTrials(): Int {
        val presentAthletes = athletes.filter { !absentAthletes.contains(it.id) }
        if (presentAthletes.isEmpty()) return 0
        return presentAthletes.minOf { completionState[it.id] ?: 0 }
    }

    private suspend fun saveResult(athlete: Individual, timeMs: Long) {
        // Obsolete but kept for signature matching if needed elsewhere, 
        // though we now use submitPending
    }

    fun getAllAthletes(): List<AthleteQueueItem> {
        val count = athletes.size
        if (count == 0) return emptyList()
        val pendingResults = _uiState.value.pendingResults
        
        return athletes.map { athlete ->
            val done = completionState[athlete.id] ?: 0
            val pendingScore = pendingResults[athlete.id]
            val isPending = pendingScore != null
            val isAbsent = absentAthletes.contains(athlete.id)
            
            val status = when {
                isAbsent -> AthleteStatus.ABSENT
                done >= totalTrialsPerAthlete -> AthleteStatus.COMPLETED
                isPending -> AthleteStatus.CAPTURED
                else -> AthleteStatus.WAITING
            }
            
            AthleteQueueItem(
                athleteId = athlete.id,
                name = athlete.fullName,
                currentTrial = minOf(totalTrialsPerAthlete, done + (if (isPending) 1 else 0) + 1),
                totalTrials = totalTrialsPerAthlete,
                status = status,
                capturedTimeMs = if (isPending) (pendingScore!! * 1000).toLong() else null,
                historicalTrials = _uiState.value.historicalResults[athlete.id] ?: emptyList()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        confirmationJob?.cancel()
    }
}
