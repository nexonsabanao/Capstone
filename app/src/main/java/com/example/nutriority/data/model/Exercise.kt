package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var imageName: String = "",
    var targetMuscle: String = "",
    var equipment: String = "",
    var difficulty: String = "",
    var tips: String = "",
    var category: String = "",
    
    @Ignore
    var imageResId: Int = 0
)
