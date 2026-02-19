package com.example.alearning.data.mapper.people

import com.example.alearning.data.local.entities.people.IndividualEntity
import com.example.alearning.domain.model.people.Individual

fun IndividualEntity.toDomain(): Individual {
    return Individual(
        id = this.id,
        firstName = this.firstName,
        lastName = this.lastName,
        dateOfBirth = this.dateOfBirth,
        sex = this.sex,
        medicalAlert = this.medicalAlert,
        isRestricted = this.isRestricted,
        email = this.email,
        isActive = this.isActive,
        notes = this.notes
    )
}

fun Individual.toEntity(
    createdAt: Long = System.currentTimeMillis()
): IndividualEntity {
    return IndividualEntity(
        id = this.id,
        firstName = this.firstName,
        lastName = this.lastName,
        dateOfBirth = this.dateOfBirth,
        sex = this.sex,
        medicalAlert = this.medicalAlert,
        isRestricted = this.isRestricted,
        email = this.email,
        isActive = this.isActive,
        notes = this.notes,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
        isDeleted = false
    )
}