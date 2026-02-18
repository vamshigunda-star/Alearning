package com.example.alearning.domain.usecase.standards

import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import com.example.alearning.domain.repository.StandardsRepository
import javax.inject.Inject

class ImportStandardsUseCase @Inject constructor(
    private val repository: StandardsRepository
) {
    suspend operator fun invoke(
        categories: List<TestCategoryEntity>,
        tests: List<FitnessTestEntity>,
        norms: List<NormReferenceEntity>
    ) {
        repository.importStandards(categories, tests, norms)
    }
}
