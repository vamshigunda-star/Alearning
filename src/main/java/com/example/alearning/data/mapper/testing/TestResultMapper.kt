package com.example.alearning.data.mapper.testing

import com.example.alearning.data.local.entities.testing.TestResultEntity
import com.example.alearning.domain.model.testing.TestResult

fun TestResultEntity.toDomain(): TestResult {
    return TestResult(
        id = this.id,
        eventId = this.eventId,
        individualId = this.individualId,
        testId = this.testId,
        rawScore = this.rawScore,
        ageAtTime = this.ageAtTime,
        weightAtTime = this.weightAtTime,
        bodyWeightKg = this.bodyWeightKg,
        percentile = this.percentile,
        classification = this.classification,
        normVariantUsed = this.normVariantUsed,
        createdAt = this.createdAt
    )
}

fun TestResult.toEntity(): TestResultEntity {
    return TestResultEntity(
        id = this.id,
        eventId = this.eventId,
        individualId = this.individualId,
        testId = this.testId,
        rawScore = this.rawScore,
        ageAtTime = this.ageAtTime,
        weightAtTime = this.weightAtTime,
        bodyWeightKg = this.bodyWeightKg,
        percentile = this.percentile,
        classification = this.classification,
        normVariantUsed = this.normVariantUsed,
        createdAt = this.createdAt
    )
}
