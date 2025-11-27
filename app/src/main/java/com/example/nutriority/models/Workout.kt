// CORRECT - This will fix the error
package com.example.nutriority.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey // Make sure this import exists

@Entity(tableName = "workouts")
data class Workout(
    // The @PrimaryKey annotation MUST be on its own line to be recognized.
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val description: String,
    val category: String,
    val targetMuscle: String,
    val imageName: String
) {
    @Ignore
    var imageResId: Int = 0
}
