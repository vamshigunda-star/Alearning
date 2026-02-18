package com.example.alearning.domain.usecase.people

import com.example.alearning.data.local.entities.people.GroupEntity
import com.example.alearning.domain.repository.PeopleRepository
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val repository: PeopleRepository
) {
    suspend operator fun invoke(
        name: String,
        location: String? = null,
        cycle: String? = null,
        category: String? = null
    ): Result<GroupEntity> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Group name is required"))

        val group = GroupEntity(
            name = name.trim(),
            location = location?.trim(),
            cycle = cycle?.trim(),
            category = category?.trim()
        )
        repository.insertGroup(group)
        return Result.success(group)
    }
}
