package com.example.alearning.domain.model.testing

data class TestResult(
    val id: String,
    val eventId: String,
    val individualId: String,
    val testId: String,
    val rawScore: Double,

    // Snapshot fields for historical accuracy
    val ageAtTime: Float,
    val weightAtTime: Double? = null,
    val bodyWeightKg: Double? = null,

    // Interpretation
    val percentile: Int? = null,
    val classification: String? = null,
    val normVariantUsed: String? = null
)
