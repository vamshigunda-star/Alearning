package com.example.alearning.data.mapper.testing

import com.example.alearning.data.local.entities.testing.TestingEventEntity
import com.example.alearning.domain.model.testing.TestingEvent

fun TestingEventEntity.toDomain(): TestingEvent {
    return TestingEvent(
        id = this.id,
        groupId = this.groupId,
        name = this.name,
        date = this.date,
        location = this.location,
        notes = this.notes
    )
}

fun TestingEvent.toEntity(
    createdAt: Long = System.currentTimeMillis()
): TestingEventEntity {
    return TestingEventEntity(
        id = this.id,
        groupId = this.groupId,
        name = this.name,
        date = this.date,
        location = this.location,
        notes = this.notes,
        createdAt = createdAt
    )
}
