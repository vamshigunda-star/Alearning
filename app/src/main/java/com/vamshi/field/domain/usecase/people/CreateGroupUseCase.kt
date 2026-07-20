package com.example.alearning.domain.usecase.people

import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.people.GroupCategory
import com.example.alearning.domain.repository.PeopleRepository
import java.util.UUID
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val repository: PeopleRepository
) {
    suspend operator fun invoke(
        name: String,
        location: String? = null,
        cycle: String? = null,
        category: GroupCategory? = null
    ): Result<Group> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Group name is required"))

        val group = Group(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            location = location?.trim(),
            cycle = cycle?.trim(),
            category = category
        )
        repository.insertGroup(group)
        return Result.success(group)
    }
}
