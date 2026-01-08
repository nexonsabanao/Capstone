package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String = "",
    val description: String = "",
    val category: String = "",
    val targetMuscle: String = "",
    val imageName: String = "img_balanced_diet", // Fixed: Added default value to prevent crashes
    val difficulty: String = "",
    val duration: String = "",
    val metValue: Double = 5.0,
    val tags: List<String> = emptyList()
) {
    @Ignore
    var imageResId: Int = 0
    @Ignore
    var warmup: List<Warmup> = emptyList()
    @Ignore
    var cooldown: List<Cooldown> = emptyList()
}