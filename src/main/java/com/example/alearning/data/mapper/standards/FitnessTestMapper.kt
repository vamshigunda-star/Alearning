package com.example.alearning.data.mapper.standards

import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.InterpretationStrategy
import com.example.alearning.domain.model.standards.TimingMode
import com.example.alearning.domain.model.standards.InputParadigm

fun FitnessTestEntity.toDomain(): FitnessTest {
    return FitnessTest(
        id = this.id,
        categoryId = this.categoryId,
        name = this.name,
        unit = this.unit,
        isHigherBetter = this.isHigherBetter,
        description = this.description,
        timingMode = try { TimingMode.valueOf(this.timingMode) } catch (_: Exception) { TimingMode.MANUAL_ENTRY },
        inputParadigm = try { InputParadigm.valueOf(this.inputParadigm) } catch (_: Exception) { InputParadigm.NUMERIC },
        athletesPerHeat = this.athletesPerHeat,
        trialsPerAthlete = this.trialsPerAthlete,
        validMin = this.validMin,
        validMax = this.validMax,
        interpretationStrategy = try {
            InterpretationStrategy.valueOf(this.interpretationStrategy)
        } catch (_: Exception) { InterpretationStrategy.NORM_LOOKUP },
        calculationConfig = this.calculationConfig
    )
}

fun FitnessTest.toEntity(
    createdAt: Long = System.currentTimeMillis()
): FitnessTestEntity {
    return FitnessTestEntity(
        id = this.id,
        categoryId = this.categoryId,
        name = this.name,
        unit = this.unit,
        isHigherBetter = this.isHigherBetter,
        description = this.description,
        timingMode = this.timingMode.name,
        inputParadigm = this.inputParadigm.name,
        athletesPerHeat = this.athletesPerHeat,
        trialsPerAthlete = this.trialsPerAthlete,
        validMin = this.validMin,
        validMax = this.validMax,
        interpretationStrategy = this.interpretationStrategy.name,
        calculationConfig = this.calculationConfig,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
        isDeleted = false
    )
}
