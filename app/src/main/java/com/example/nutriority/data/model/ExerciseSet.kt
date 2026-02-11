package com.example.nutriority.data.model

data class ExerciseSet(
    val id: String, // Stable ID: workoutId_exerciseId_category_setNumber
    var setNumber: Int = 0,
    val value: Int,
    var isActive: Boolean = false,
    var isCompleted: Boolean = false,
    val isDuration: Boolean = false
)
