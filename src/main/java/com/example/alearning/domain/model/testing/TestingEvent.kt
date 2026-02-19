package com.example.alearning.domain.model.testing

data class TestingEvent(
    val id: String,
    val groupId: String? = null,
    val name: String,
    val date: Long,
    val location: String? = null,
    val notes: String? = null
)
