package com.example.nutriority.Models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,               // The name of the workout, e.g., "Calisthenics Basics"
    val description: String,        // A short description, e.g., "Master the fundamentals of bodyweight training."
    val category: String,           // e.g., "Beginner", "Strength", "Cardio"
    val targetMuscle: String,       // The main muscle group, e.g., "Full Body", "Upper Body"
    val imageResId: Int
)
