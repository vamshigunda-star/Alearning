package com.example.alearning.domain.model.standards

data class FitnessTest(
    val id: String,
    val categoryId: String,
    val name: String,
    val unit: String,
    val isHigherBetter: Boolean,
    val description: String? = null
)
