package com.example.nutriority.data.remote.dto

import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout

data class WorkoutResponse(
    val workout: Workout,
    val exercises: List<Exercise>
)