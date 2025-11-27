package com.example.nutriority.Models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = Workout::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE // If a workout is deleted, its exercises are also deleted
    )]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val workoutId: Int,             // Foreign key to link to the Workout table

    val name: String,               // The name of the exercise, e.g., "Push-Ups"
    val description: String,        // A short description of the exercise
    val reps: String,               // e.g., "3 sets of 12 reps"
    val targetMuscle: String,       // The specific muscles targeted, e.g., "Chest, Shoulders, Triceps"
    val imageResId: Int             // The ID of the drawable resource
)
