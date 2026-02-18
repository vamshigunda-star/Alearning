package com.example.alearning.domain.usecase.testing

import com.example.alearning.data.local.entities.testing.TestingEventEntity
import com.example.alearning.domain.repository.TestingRepository
import javax.inject.Inject

class CreateEventUseCase @Inject constructor(
    private val repository: TestingRepository
) {
    suspend operator fun invoke(
        name: String,
        date: Long,
        groupId: String?,
        testIds: List<String>,
        location: String? = null,
        notes: String? = null
    ): Result<TestingEventEntity> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Event name is required"))
        if (testIds.isEmpty()) return Result.failure(IllegalArgumentException("At least one test must be selected"))

        val event = TestingEventEntity(
            name = name.trim(),
            date = date,
            groupId = groupId,
            location = location?.trim(),
            notes = notes?.trim()
        )
        repository.createEvent(event, testIds)
        return Result.success(event)
    }
}
