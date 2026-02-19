package com.example.alearning.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.repository.StandardsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestSummary(
    val testName: String,
    val latestScore: Double,
    val percentile: Int?,
    val classification: String?,
    val unit: String
)

data class StudentReportUiState(
    val student: Individual? = null,
    val testSummaries: List<TestSummary> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StudentReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val peopleRepository: PeopleRepository,
    private val testingRepository: TestingRepository,
    private val standardsRepository: StandardsRepository
) : ViewModel() {

    private val studentId: String = savedStateHandle["studentId"] ?: ""

    private val _uiState = MutableStateFlow(StudentReportUiState())
    val uiState: StateFlow<StudentReportUiState> = _uiState.asStateFlow()

    init {
        loadStudentData()
    }

    private fun loadStudentData() {
        viewModelScope.launch {
            val student = peopleRepository.getIndividualById(studentId)
            _uiState.value = _uiState.value.copy(student = student)

            if (student != null) {
                loadTestResults(student)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun loadTestResults(student: Individual) {
        // Get all categories and tests to build summaries
        val categories = standardsRepository.getAllCategories().first()
        val summaries = mutableListOf<TestSummary>()

        for (category in categories) {
            val tests = standardsRepository.getTestsByCategory(category.id).first()
            for (test in tests) {
                val history = testingRepository.getHistoryForTest(student.id, test.id).first()
                if (history.isNotEmpty()) {
                    val latest = history.last()
                    summaries.add(
                        TestSummary(
                            testName = test.name,
                            latestScore = latest.rawScore,
                            percentile = latest.percentile,
                            classification = latest.classification,
                            unit = test.unit
                        )
                    )
                }
            }
        }

        _uiState.value = _uiState.value.copy(testSummaries = summaries)
    }
}
