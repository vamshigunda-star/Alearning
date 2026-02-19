package com.example.alearning.data.mapper.standards

import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.domain.model.standards.NormReference

fun NormReferenceEntity.toDomain(): NormReference {
    return NormReference(
        id = this.id,
        testId = this.testId,
        variant = this.variant,
        sex = this.sex,
        ageMin = this.ageMin,
        ageMax = this.ageMax,
        minScore = this.minScore,
        maxScore = this.maxScore,
        percentile = this.percentile,
        classification = this.classification
    )
}

fun NormReference.toEntity(
    createdAt: Long = System.currentTimeMillis()
): NormReferenceEntity {
    return NormReferenceEntity(
        id = this.id,
        testId = this.testId,
        variant = this.variant,
        sex = this.sex,
        ageMin = this.ageMin,
        ageMax = this.ageMax,
        minScore = this.minScore,
        maxScore = this.maxScore,
        percentile = this.percentile,
        classification = this.classification,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )
}
