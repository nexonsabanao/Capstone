package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workoutId: Int,
    val exerciseName: String = "",
    val date: Date,
    val reps: String, // e.g., "12,11,10"
    val weightKg: Double = 0.0
)
