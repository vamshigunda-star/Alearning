package com.example.alearning.domain.usecase.people

import com.example.alearning.data.local.entities.people.BiologicalSex
import com.example.alearning.data.local.entities.people.IndividualEntity
import com.example.alearning.domain.repository.PeopleRepository
import javax.inject.Inject

class RegisterStudentUseCase @Inject constructor(
    private val repository: PeopleRepository
) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        dateOfBirth: Long,
        sex: BiologicalSex,
        email: String? = null,
        medicalAlert: String? = null
    ): Result<IndividualEntity> {
        if (firstName.isBlank()) return Result.failure(IllegalArgumentException("First name is required"))
        if (lastName.isBlank()) return Result.failure(IllegalArgumentException("Last name is required"))
        if (dateOfBirth <= 0) return Result.failure(IllegalArgumentException("Valid date of birth is required"))

        val student = IndividualEntity(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            dateOfBirth = dateOfBirth,
            sex = sex,
            email = email?.trim(),
            medicalAlert = medicalAlert?.trim()
        )
        repository.insertIndividual(student)
        return Result.success(student)
    }
}
