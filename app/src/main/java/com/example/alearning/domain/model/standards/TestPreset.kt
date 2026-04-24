package com.example.alearning.domain.model.standards

data class TestPreset(
    val id: String,
    val name: String,
    val description: String?,
    val testIds: List<String>,
    val isBuiltIn: Boolean
)
