package com.example.alearning.domain.model.people

enum class GroupCategory { TEAM, CLASS, PERSONAL_TRAINING }

data class Group(
    val id: String,

    // 1. Group Details
    val name: String,
    val location: String?,

    // 2. Organization
    val cycle: String?,
    val category: GroupCategory?
)
