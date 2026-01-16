package com.example.nutriority.data.model

import java.util.UUID

data class ExerciseSet(
    val id: String = UUID.randomUUID().toString(),
    var setNumber: Int = 0,
    val value: Int,
    var isActive: Boolean = false,
    var isCompleted: Boolean = false,
    val isDuration: Boolean = false
)
