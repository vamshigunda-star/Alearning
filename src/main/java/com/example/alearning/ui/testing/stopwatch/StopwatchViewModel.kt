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

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var confirmationJob: Job? = null
    private var startNanos: Long = 0L
    private var lastSavedResultId: String? = null

    // Full queue of athletes with their trial progress
    private var athleteQueue: List<AthleteQueueEntry> = emptyList()
    private var currentQueueIndex: Int = 0

    // For GROUP_START mode
    private var heats: List<List<Individual>> = emptyList()
    private var currentHeatIndex: Int = 0
    private var heatFinishIndex: Int = 0

    private data class AthleteQueueEntry(
        val athlete: Individual,
        val currentTrial: Int,
        val totalTrials: Int
    )

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            try {
                val session = stopwatchSessionUseCase(eventId, fitnessTestId, groupId)
                val test = session.fitnessTest

                // Build queue: each athlete appears trialsPerAthlete times
                // but subtract already-completed trials
                val queue = mutableListOf<AthleteQueueEntry>()
                for (athlete in session.athletes) {
                    val completed = session.trialCounts[athlete.id] ?: 0
                    for (trial in (completed + 1)..session.trialsPerAthlete) {
                        queue.add(AthleteQueueEntry(athlete, trial, session.trialsPerAthlete))
                    }
                }
                athleteQueue = queue
                heats = session.heats

                val totalAthletes = session.athletes.size
                val totalTrials = totalAthletes * session.trialsPerAthlete
                val completedTrials = session.trialCounts.values.sum()

                // Load event name
                val event = testingRepository.getEventById(eventId)

                when (test.timingMode) {
                    TimingMode.INDIVIDUAL -> {
                        val currentEntry = queue.firstOrNull()
                        _uiState.update {
                            it.copy(
                                mode = TimingMode.INDIVIDUAL,
                                eventName = event?.name ?: "",
                                testName = test.name,
                                testUnit = test.unit,
                                currentAthlete = currentEntry?.toQueueItem(AthleteStatus.ACTIVE),
                                completedCount = completedTrials,
                                totalCount = totalTrials,
                                isLoading = false,
                                isSessionComplete = queue.isEmpty()
                            )
                        }
                    }
                    TimingMode.GROUP_START -> {
                        // Find first heat that has incomplete athletes
                        currentHeatIndex = findNextIncompleteHeat(session)
                        _uiState.update {
                            it.copy(
                                mode = TimingMode.GROUP_START,
                                eventName = event?.name ?: "",
                                testName = test.name,
                                testUnit = test.unit,
                                heatAthletes = buildHeatAthletesList(),
                                currentHeatNumber = currentHeatIndex + 1,
                                totalHeats = heats.size,
                                completedCount = completedTrials,
                                totalCount = totalTrials,
                                isLoading = false,
                                isSessionComplete = queue.isEmpty()
                            )
                        }
                    }
                    TimingMode.MANUAL_ENTRY -> {
                        _uiState.update {
                            it.copy(isLoading = false, isSessionComplete = true)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message, isLoading = false)
                }
            }
        }
    }

    private fun findNextIncompleteHeat(session: com.example.alearning.domain.usecase.testing.StopwatchSession): Int {
        for (i in heats.indices) {
            val hasIncomplete = heats[i].any { athlete ->
                (session.trialCounts[athlete.id] ?: 0) < session.trialsPerAthlete
            }
            if (hasIncomplete) return i
        }
        return 0
    }

    private fun buildHeatAthletesList(): List<AthleteQueueItem> {
        if (currentHeatIndex >= heats.size) return emptyList()
        return heats[currentHeatIndex].map { athlete ->
            val entry = athleteQueue.find { it.athlete.id == athlete.id }
            AthleteQueueItem(
                athleteId = athlete.id,
                name = athlete.fullName,
                currentTrial = entry?.currentTrial ?: 1,
                totalTrials = entry?.totalTrials ?: 1,
                status = if (entry != null) AthleteStatus.WAITING else AthleteStatus.COMPLETED
            )
        }
    }

    fun onAction(action: StopwatchAction) {
        when (action) {
            is StopwatchAction.OnStartStop -> handleStartStop()
            is StopwatchAction.OnFinishTap -> handleFinishTap()
            is StopwatchAction.OnStopHeat -> handleStopHeat()
            is StopwatchAction.OnUndo -> handleUndo()
            is StopwatchAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            is StopwatchAction.OnNavigateBack -> Unit // handled by screen
        }
    }

    private fun handleStartStop() {
        val phase = _uiState.value.stopwatchPhase
        when {
            phase == StopwatchPhase.READY -> startTimer()
            phase == StopwatchPhase.RUNNING && _uiState.value.mode == TimingMode.INDIVIDUAL -> stopAndSaveIndividual()
            phase == StopwatchPhase.RUNNING && _uiState.value.mode == TimingMode.GROUP_START -> {
                // In group mode, the main button starts. Finish taps capture individual athletes.
                // This shouldn't be called during RUNNING for group mode; use OnFinishTap instead.
            }
        }
    }

    private fun startTimer() {
        startNanos = System.nanoTime()
        _uiState.update { it.copy(stopwatchPhase = StopwatchPhase.RUNNING, elapsedMs = 0L) }
        heatFinishIndex = 0

        // Mark active athletes
        if (_uiState.value.mode == TimingMode.GROUP_START) {
            _uiState.update { state ->
                state.copy(
                    heatAthletes = state.heatAthletes.map { athlete ->
                        if (athlete.status == AthleteStatus.WAITING) {
                            athlete.copy(status = AthleteStatus.ACTIVE)
                        } else athlete
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

    private fun stopAndSaveIndividual() {
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        tickerJob?.cancel()

        val entry = athleteQueue.getOrNull(currentQueueIndex) ?: return
        val athlete = entry.athlete

        _uiState.update {
            it.copy(
                stopwatchPhase = StopwatchPhase.CONFIRMING,
                elapsedMs = elapsedMs,
                confirmationData = ConfirmationData(
                    athleteName = athlete.fullName,
                    timeMs = elapsedMs,
                    trialNumber = entry.currentTrial
                ),
                canUndo = true
            )
        }

        viewModelScope.launch {
            saveResult(athlete, elapsedMs)
        }

        confirmationJob = viewModelScope.launch {
            delay(1500)
            advanceToNextIndividual()
        }
    }

    private fun handleFinishTap() {
        if (_uiState.value.mode != TimingMode.GROUP_START) return
        if (_uiState.value.stopwatchPhase != StopwatchPhase.RUNNING) return

        val currentHeat = heats.getOrNull(currentHeatIndex) ?: return
        val activeAthletes = _uiState.value.heatAthletes.filter { it.status == AthleteStatus.ACTIVE }
        if (activeAthletes.isEmpty()) return

        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        val capturedAthlete = activeAthletes.first()

        // Update the heat list to show captured time
        _uiState.update { state ->
            state.copy(
                heatAthletes = state.heatAthletes.map { athlete ->
                    if (athlete.athleteId == capturedAthlete.athleteId && athlete.status == AthleteStatus.ACTIVE) {
                        athlete.copy(status = AthleteStatus.CAPTURED, capturedTimeMs = elapsedMs)
                    } else athlete
                }
            )
        }

        // Save the result
        val individual = currentHeat.find { it.id == capturedAthlete.athleteId } ?: return
        viewModelScope.launch {
            saveResult(individual, elapsedMs)
        }

        heatFinishIndex++

        // Check if all athletes in heat are captured
        val remainingActive = _uiState.value.heatAthletes.count { it.status == AthleteStatus.ACTIVE }
        if (remainingActive == 0) {
            finishHeat(capturedAthlete.name, elapsedMs)
        }
    }

    private fun handleStopHeat() {
        if (_uiState.value.stopwatchPhase != StopwatchPhase.RUNNING) return
        tickerJob?.cancel()

        val lastCaptured = _uiState.value.heatAthletes.lastOrNull { it.status == AthleteStatus.CAPTURED }

        // Mark remaining ACTIVE athletes as DNF (they don't get a result saved)
        _uiState.update { state ->
            state.copy(
                heatAthletes = state.heatAthletes.map { athlete ->
                    if (athlete.status == AthleteStatus.ACTIVE) {
                        athlete.copy(status = AthleteStatus.WAITING) // Reset to waiting for retry
                    } else athlete
                }
            )
        }

        if (lastCaptured != null) {
            finishHeat(lastCaptured.name, lastCaptured.capturedTimeMs ?: 0L)
        } else {
            // No one was captured, just reset
            _uiState.update { it.copy(stopwatchPhase = StopwatchPhase.READY) }
        }
    }

    private fun finishHeat(lastAthleteName: String, lastTimeMs: Long) {
        tickerJob?.cancel()

        _uiState.update {
            it.copy(
                stopwatchPhase = StopwatchPhase.CONFIRMING,
                confirmationData = ConfirmationData(
                    athleteName = lastAthleteName,
                    timeMs = lastTimeMs,
                    trialNumber = 0 // heat mode
                ),
                canUndo = false // Undo is complex for multi-capture; disabled for heats
            )
        }

        confirmationJob = viewModelScope.launch {
            delay(1500)
            advanceToNextHeat()
        }
    }

    private fun advanceToNextIndividual() {
        currentQueueIndex++
        if (currentQueueIndex >= athleteQueue.size) {
            _uiState.update {
                it.copy(
                    stopwatchPhase = StopwatchPhase.READY,
                    confirmationData = null,
                    canUndo = false,
                    isSessionComplete = true,
                    completedCount = it.totalCount,
                    currentAthlete = null
                )
            }
            return
        }

        val nextEntry = athleteQueue[currentQueueIndex]
        _uiState.update {
            it.copy(
                stopwatchPhase = StopwatchPhase.READY,
                elapsedMs = 0L,
                currentAthlete = nextEntry.toQueueItem(AthleteStatus.ACTIVE),
                confirmationData = null,
                canUndo = false,
                completedCount = currentQueueIndex
            )
        }
    }

    private fun advanceToNextHeat() {
        currentHeatIndex++
        if (currentHeatIndex >= heats.size) {
            _uiState.update {
                it.copy(
                    stopwatchPhase = StopwatchPhase.READY,
                    confirmationData = null,
                    isSessionComplete = true,
                    completedCount = it.totalCount
                )
            }
            return
        }

        heatFinishIndex = 0
        _uiState.update {
            it.copy(
                stopwatchPhase = StopwatchPhase.READY,
                elapsedMs = 0L,
                heatAthletes = buildHeatAthletesList(),
                currentHeatNumber = currentHeatIndex + 1,
                confirmationData = null,
                completedCount = it.completedCount + heatFinishIndex
            )
        }
    }

    private fun handleUndo() {
        val resultId = lastSavedResultId ?: return
        confirmationJob?.cancel()

        viewModelScope.launch {
            try {
                testingRepository.deleteResultById(resultId)
                lastSavedResultId = null

                // Revert to current athlete, ready state
                if (_uiState.value.mode == TimingMode.INDIVIDUAL) {
                    val entry = athleteQueue.getOrNull(currentQueueIndex) ?: return@launch
                    _uiState.update {
                        it.copy(
                            stopwatchPhase = StopwatchPhase.READY,
                            elapsedMs = 0L,
                            currentAthlete = entry.toQueueItem(AthleteStatus.ACTIVE),
                            confirmationData = null,
                            canUndo = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Undo failed: ${e.message}") }
            }
        }
    }

    private suspend fun saveResult(athlete: Individual, timeMs: Long) {
        try {
            val ageMillis = System.currentTimeMillis() - athlete.dateOfBirth
            val ageYears = (ageMillis / (365.25 * 24 * 60 * 60 * 1000)).toFloat()

            // Convert milliseconds to seconds for rawScore (stored as Double)
            val rawScoreSeconds = timeMs / 1000.0

            val result = recordResult(
                eventId = eventId,
                individualId = athlete.id,
                testId = fitnessTestId,
                rawScore = rawScoreSeconds,
                ageAtTime = ageYears,
                sex = athlete.sex,
                captureMethod = CaptureMethod.STOPWATCH
            )
            lastSavedResultId = result.id
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Save failed: ${e.message}") }
        }
    }

    private fun AthleteQueueEntry.toQueueItem(status: AthleteStatus) = AthleteQueueItem(
        athleteId = athlete.id,
        name = athlete.fullName,
        currentTrial = currentTrial,
        totalTrials = totalTrials,
        status = status
    )

    fun getUpcomingAthletes(): List<AthleteQueueItem> {
        if (_uiState.value.mode != TimingMode.INDIVIDUAL) return emptyList()
        return athleteQueue.drop(currentQueueIndex + 1).take(5).map {
            it.toQueueItem(AthleteStatus.WAITING)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        confirmationJob?.cancel()
    }
}
