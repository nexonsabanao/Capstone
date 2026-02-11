package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "workout_exercises",
    primaryKeys = ["workoutId", "exerciseId", "category"],
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId"), Index("exerciseId")]
)
data class WorkoutExercise(
    var workoutId: Int = 0,
    var exerciseId: String = "",
    var category: String = "",
    var sets: Int = 0,
    var reps: String = "",
    var rest: String = "",
    var duration: String = "",
    var order: Int = 0,
    var isCompleted: Boolean = false
)
