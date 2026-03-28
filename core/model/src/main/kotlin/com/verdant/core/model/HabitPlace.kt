package com.verdant.core.model

data class HabitPlace(
    val id: String,
    val habitId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radiusMeters: Double,
    val triggerOn: String,
)
