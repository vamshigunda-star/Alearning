package com.example.alearning.domain.usecase.people

import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.repository.PeopleRepository
import java.util.UUID
import javax.inject.Inject

class RegisterAthleteUseCase @Inject constructor(
    private val repository: PeopleRepository
) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        dateOfBirth: Long,
        sex: BiologicalSex,
        email: String? = null,
        medicalAlert: String? = null
    ): Result<Individual> {
        if (firstName.isBlank()) return Result.failure(IllegalArgumentException("First name is required"))
        if (lastName.isBlank()) return Result.failure(IllegalArgumentException("Last name is required"))
        if (dateOfBirth <= 0) return Result.failure(IllegalArgumentException("Valid date of birth is required"))

        val student = Individual(
            id = UUID.randomUUID().toString(),
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
