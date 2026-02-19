package com.example.alearning.data.mapper.standards

import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import com.example.alearning.domain.model.standards.TestCategory

fun TestCategoryEntity.toDomain(): TestCategory {
    return TestCategory(
        id = this.id,
        name = this.name,
        sortOrder = this.sortOrder
    )
}

fun TestCategory.toEntity(
    createdAt: Long = System.currentTimeMillis()
): TestCategoryEntity {
    return TestCategoryEntity(
        id = this.id,
        name = this.name,
        sortOrder = this.sortOrder,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
        isDeleted = false
    )
}
