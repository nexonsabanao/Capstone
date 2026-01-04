package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
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
    indices = [Index(value = ["workoutId"])]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    var order: Int = 0,
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val rest: String = "",
    val duration: String = "",
    val description: String = "",
    val imageName: String = "",
    val targetMuscle: String = "",
    var category: String = "Exercise", // Changed to var to allow reassignment

    var workoutId: Int = 0
) {
    @Ignore
    var imageResId: Int = 0
}
