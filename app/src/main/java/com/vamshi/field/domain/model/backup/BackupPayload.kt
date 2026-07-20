package com.vamshi.field.domain.model.backup

data class BackupPayload(
    val individuals: List<BackupIndividual>,
    val groups: List<BackupGroup>,
    val groupMembers: List<BackupGroupMemberCrossRef>,
    val testingEvents: List<BackupTestingEvent>,
    val eventTests: List<BackupEventTestCrossRef>,
    val testResults: List<BackupTestResult>,
    val users: List<BackupUser>
)

data class BackupIndividual(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: Long,
    val gender: String,
    val notes: String?
)

data class BackupGroup(
    val id: String,
    val name: String,
    val type: String,
    val isActive: Boolean
)

data class BackupGroupMemberCrossRef(
    val groupId: String,
    val individualId: String
)

data class BackupTestingEvent(
    val id: String,
    val name: String,
    val timestamp: Long,
    val notes: String?
)

data class BackupEventTestCrossRef(
    val eventId: String,
    val testId: String
)

data class BackupTestResult(
    val id: String,
    val eventId: String,
    val individualId: String,
    val testId: String,
    val rawScore: Double,
    val standardizedScore: Double?,
    val timestamp: Long,
    val captureMethod: String,
    val notes: String?
)

data class BackupUser(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val passwordHash: String,
    val passwordSalt: String,
    val securityQuestion: String?,
    val securityAnswerHash: String?,
    val securityAnswerSalt: String?,
    val email: String? = null,
    val createdAt: Long
)
