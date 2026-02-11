package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey
    var id: String = "",
    var name: String = "",
    var category: String = "",
    var difficulty: String = "",
    var gifUrl: String = "",
    var instructions: List<String> = emptyList(),
    var secondary: String = "",
    var target: String = "",
    var bodyPart: String = "",
    
    @get:Exclude
    var description: String = "",
    @get:Exclude
    var imageName: String = "",
    @get:Exclude
    var targetMuscle: String = "",
    @get:Exclude
    var equipment: String = "",
    @get:Exclude
    var tips: String = "",
    @get:Exclude
    @Transient
    var imageResId: Int = 0
)
