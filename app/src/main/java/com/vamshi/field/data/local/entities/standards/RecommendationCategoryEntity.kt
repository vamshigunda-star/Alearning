package com.vamshi.field.data.local.entities.standards

import androidx.room.Entity
import androidx.room.PrimaryKey

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
