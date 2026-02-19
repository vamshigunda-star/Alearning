package com.example.alearning.domain.usecase.standards

import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.repository.StandardsRepository
import javax.inject.Inject

data class PercentileResult(
    val percentile: Int,
    val classification: String?
)

class CalculatePercentileUseCase @Inject constructor(
    private val repository: StandardsRepository
) {
    suspend operator fun invoke(
        testId: String,
        rawScore: Double,
        age: Double,
        sex: BiologicalSex
    ): PercentileResult? {
        val norm = repository.getNormResult(testId, sex, age, rawScore)
        return norm?.let {
            PercentileResult(
                percentile = it.percentile,
                classification = it.classification
            )
        }
    }
}
