package com.example.alearning.domain.usecase.standards

import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.NormReference
import com.example.alearning.domain.model.standards.TestCategory
import com.example.alearning.domain.repository.StandardsRepository
import javax.inject.Inject

class ImportStandardsUseCase @Inject constructor(
    private val repository: StandardsRepository
) {
    suspend operator fun invoke(
        categories: List<TestCategory>,
        tests: List<FitnessTest>,
        norms: List<NormReference>
    ) {
        repository.importStandards(categories, tests)
        repository.insertNorms(norms)
    }
}
