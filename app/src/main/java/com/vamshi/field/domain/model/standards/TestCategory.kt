package com.vamshi.field.domain.model.standards

enum class RadarAxis {
    SPEED, AGILITY, STRENGTH, ENDURANCE, FLEXIBILITY, BALANCE
}

data class TestCategory(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val radarAxis: RadarAxis? = null
)
