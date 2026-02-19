package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.model.testing.TestingEvent
import com.example.alearning.domain.repository.TestingRepository
import java.util.UUID
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
    ): Result<TestingEvent> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Event name is required"))
        if (testIds.isEmpty()) return Result.failure(IllegalArgumentException("At least one test must be selected"))

        val event = TestingEvent(
            id = UUID.randomUUID().toString(),
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
