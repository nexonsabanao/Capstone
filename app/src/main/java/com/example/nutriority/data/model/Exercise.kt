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
    indices = [Index("workoutId")]
)

data class Exercise(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var workoutId: Int = 0,
    var name: String = "",
    var description: String = "",
    var imageName: String = "",
    var duration: String = "",
    var category: String = "",
    var targetMuscle: String = "",
    var equipment: String = "",
    var sets: Int = 0,
    var reps: String = "",
    var rest: String = "",
    var tips: String = "",
    var order: Int = 0,
    var difficulty: String = "", // Added for individual exercise difficulty

    @Ignore
    var imageResId: Int = 0
)
