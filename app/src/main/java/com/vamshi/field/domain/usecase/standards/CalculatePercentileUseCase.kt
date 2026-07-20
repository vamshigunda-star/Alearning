package com.example.alearning.domain.usecase.standards

import android.util.Log
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.repository.StandardsRepository
import javax.inject.Inject

data class PercentileResult(
    val percentile: Int,
    val classification: String?,
    // Provenance of which standard set produced this percentile, e.g. "Standard 2025".
    // Null when the matched norm row had no explicit variant.
    val variant: String? = null
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
        if (norm == null) {
            Log.w(
                "CalculatePercentile",
                "No norm match: testId=$testId sex=$sex age=$age score=$rawScore"
            )
            return null
        }
        return PercentileResult(
            percentile = norm.percentile,
            classification = norm.classification,
            variant = norm.variant
        )
    }
}
