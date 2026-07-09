package com.example.alearning.data.mapper.testing

import com.example.alearning.data.local.entities.testing.PendingTestEntryEntity
import com.example.alearning.domain.model.testing.PendingTestEntry

fun PendingTestEntryEntity.toDomain(): PendingTestEntry = PendingTestEntry(
    eventId = eventId,
    individualId = individualId,
    testId = testId,
    rawScore = rawScore,
    stagedAt = stagedAt
)

fun PendingTestEntry.toEntity(): PendingTestEntryEntity = PendingTestEntryEntity(
    eventId = eventId,
    individualId = individualId,
    testId = testId,
    rawScore = rawScore,
    stagedAt = stagedAt
)
