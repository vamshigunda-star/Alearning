package com.vamshi.field.domain.model.testing

data class PendingTestEntry(
    val eventId: String,
    val individualId: String,
    val testId: String,
    val rawScore: Double,
    val stagedAt: Long
)
