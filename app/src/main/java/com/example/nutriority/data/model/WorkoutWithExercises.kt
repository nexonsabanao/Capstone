package com.example.nutriority.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutWithExercises(
    @Embedded val workout: Workout,
    @Relation(
        entity = WorkoutExercise::class,
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val exerciseAssignments: List<WorkoutExerciseWithDetail>
)
