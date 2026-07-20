package com.vamshi.field.data.mapper.testing

import com.vamshi.field.data.local.entities.testing.PendingTestEntryEntity
import com.vamshi.field.domain.model.testing.PendingTestEntry

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
