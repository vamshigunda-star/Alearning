package com.vamshi.field.domain.model.standards

data class TestPreset(
    val id: String,
    val name: String,
    val description: String?,
    val testIds: List<String>,
    val isBuiltIn: Boolean
)
