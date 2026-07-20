package com.example.alearning.data.mapper.people

import com.example.alearning.data.local.entities.people.GroupEntity
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.people.GroupCategory

fun GroupEntity.toDomain(): Group {
    return Group(
        id = this.id,
        name = this.name,
        location = this.location,
        cycle = this.cycle,
        category = this.category?.let { runCatching { GroupCategory.valueOf(it) }.getOrNull() }
    )
}

fun Group.toEntity(
    createdAt: Long = System.currentTimeMillis()
): GroupEntity {
    return GroupEntity(
        id = this.id,
        name = this.name,
        location = this.location,
        cycle = this.cycle,
        category = this.category?.name,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
        isDeleted = false
    )
}
