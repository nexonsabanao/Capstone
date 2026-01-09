package com.example.nutriority.data.model

import java.util.UUID

data class ExerciseSet(
    val id: String = UUID.randomUUID().toString(),
    var setNumber: Int = 0,
    val value: Int, // Represents either reps or duration in seconds
    var isActive: Boolean = false,
    val isDuration: Boolean = false // Flag to distinguish between reps and seconds
)
