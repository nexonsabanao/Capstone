package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    var bodyPart: String = "", // Added to match Firestore data and fix Logcat errors

    // Deprecated fields from old model - kept for local db migration if needed, but not used by Firestore
    var description: String = "",
    var imageName: String = "",
    var targetMuscle: String = "",
    var equipment: String = "",
    var tips: String = "",
    var imageResId: Int = 0
)
