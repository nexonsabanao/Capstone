// In Exercise.kt

package com.example.nutriority.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
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
    ],
    indices = [Index(value = ["workoutId"]) ]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String = "",
    // Provide defaults to make deserialization from JSON safe even when fields are missing.
    val sets: String = "",
    val reps: String = "",
    val rest: String = "",
    val description: String = "",
    val imageName: String = "",

    // Keep workoutId mutable so pre-population can assign the generated parent ID
    var workoutId: Int = 0
) {
    @Ignore
    var imageResId: Int = 0
}
