package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_session_logs")
data class WorkoutSessionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workoutId: Int,
    val workoutName: String,
    val date: Long,
    val exercisesDone: Int,
    val totalExercises: Int,
    val durationSeconds: Long,
    val caloriesBurned: Int,
    val difficulty: String,
    val weightKg: Double = 0.0 // Added to track weight history
)
