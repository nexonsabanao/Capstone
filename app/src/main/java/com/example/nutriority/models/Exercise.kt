// In Exercise.kt

package com.example.nutriority.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val sets: String,
    val reps: String,
    val rest: String,
    val description: String,
    val imageName: String,

    // THE FIX IS HERE: Change 'val' to 'var'
    var workoutId: Int = 0 // This allows it to be reassigned
) {
    @Ignore
    var imageResId: Int = 0
}
