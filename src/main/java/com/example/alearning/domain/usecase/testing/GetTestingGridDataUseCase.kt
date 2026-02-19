package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class TestingGridData(
    val students: List<Individual>,
    val tests: List<FitnessTest>,
    val results: List<TestResult>
)

class GetTestingGridDataUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val testingRepository: TestingRepository
) {
    fun invoke(eventId: String, groupId: String): Flow<TestingGridData> {
        val studentsFlow = peopleRepository.getIndividualsInGroup(groupId)
        val testsFlow = testingRepository.getTestsForEvent(eventId)
        val resultsFlow = testingRepository.getEventResults(eventId)

        return combine(studentsFlow, testsFlow, resultsFlow) { students, tests, results ->
            TestingGridData(
                students = students,
                tests = tests,
                results = results
            )
        }
    }
}
