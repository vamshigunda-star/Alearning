package com.example.alearning.data.mapper.standards

import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.domain.model.standards.FitnessTest

fun FitnessTestEntity.toDomain(): FitnessTest {
    return FitnessTest(
        id = this.id,
        categoryId = this.categoryId,
        name = this.name,
        unit = this.unit,
        isHigherBetter = this.isHigherBetter,
        description = this.description
    )
}

fun FitnessTest.toEntity(
    createdAt: Long = System.currentTimeMillis()
): FitnessTestEntity {
    return FitnessTestEntity(
        id = this.id,
        categoryId = this.categoryId,
        name = this.name,
        unit = this.unit,
        isHigherBetter = this.isHigherBetter,
        description = this.description,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
        isDeleted = false
    )
}
