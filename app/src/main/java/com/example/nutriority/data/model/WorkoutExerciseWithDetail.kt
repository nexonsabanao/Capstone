package com.example.nutriority.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutExerciseWithDetail(
    @Embedded val assignment: WorkoutExercise,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: Exercise
)
