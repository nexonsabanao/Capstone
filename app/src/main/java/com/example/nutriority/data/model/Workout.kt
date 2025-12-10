package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class Workout(
    // The @PrimaryKey annotation MUST be on its own line to be recognized.
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val description: String,
    val category: String,
    val targetMuscle: String,
    val imageName: String,
    val difficulty: String,
    val duration: String
) {
    @Ignore
    var imageResId: Int = 0
}