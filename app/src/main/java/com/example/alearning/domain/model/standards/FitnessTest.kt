package com.example.alearning.domain.model.standards

enum class InputParadigm {
    NUMERIC,      // Basic keypad entry (Distance, Weight)
    INCREMENTAL,  // +/- Buttons (Pushups, Situps)
    CHRONO,       // Stopwatch/Timing (Sprints)
    MULTI_STAGE,  // Level/Shuttle (Beep Test)
    SCALE         // 1-10 scores (RPE)
}

data class FitnessTest(
    val id: String,
    val categoryId: String,
    val name: String,
    val unit: String,
    val isHigherBetter: Boolean,
    val description: String? = null,
    val timingMode: TimingMode = TimingMode.MANUAL_ENTRY,
    val inputParadigm: InputParadigm = InputParadigm.NUMERIC, // Driving modular UI
    val athletesPerHeat: Int? = null,
    val trialsPerAthlete: Int = 1
)
