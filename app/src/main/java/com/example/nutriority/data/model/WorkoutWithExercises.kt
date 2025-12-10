package com.example.nutriority.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutWithExercises(
    @Embedded val workout: Workout,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val exercises: List<Exercise>
)