package com.vamshi.field.data.local.entities.standards

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "recommendation_test_cross_ref",
    primaryKeys = ["recommendationCategoryId", "testId"],
    foreignKeys = [
        ForeignKey(
            entity = RecommendationCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["recommendationCategoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FitnessTestEntity::class,
            parentColumns = ["id"],
            childColumns = ["testId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("testId")]
)
data class RecommendationTestCrossRef(
    val recommendationCategoryId: String,
    val testId: String,
    val sortOrder: Int = 0,
    val required: Boolean = true
)
