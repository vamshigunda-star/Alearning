package com.vamshi.field.domain.usecase.standards

import com.vamshi.field.domain.model.standards.FitnessTest
import com.vamshi.field.domain.model.standards.NormReference
import com.vamshi.field.domain.model.standards.TestCategory
import com.vamshi.field.domain.repository.StandardsRepository
import javax.inject.Inject

/**
 * Imports the read-only test catalog (categories, tests, norms).
 *
 * Non-destructive by design: user-generated testing data (events, results) references
 * catalog tests, so catalog rows are upserted in place rather than deleted and re-inserted.
 * Norms have no dependents and no stable natural key, so they are replaced wholesale.
 * Catalog rows removed from a newer import are intentionally left in place — existing
 * results may still reference them.
 */
class ImportStandardsUseCase @Inject constructor(
    private val repository: StandardsRepository
) {
    suspend operator fun invoke(
        categories: List<TestCategory>,
        tests: List<FitnessTest>,
        norms: List<NormReference>
    ) {
        repository.importStandards(categories, tests)
        repository.replaceAllNorms(norms)
    }
}
