package com.vamshi.field.data.local.entities.standards

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "recommendation_categories")
data class RecommendationCategoryEntity(
    @PrimaryKey
    val id: String,

    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val scope: String = "POPULATION", // POPULATION, SPORT, ORG, CUSTOM, AI
    val sortOrder: Int = 0
)
