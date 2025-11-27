package com.example.nutriority.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "user_profile")
@TypeConverters(Converters::class)
data class User(@PrimaryKey(autoGenerate = false)
                val id: Int = 1,
                val gender: String = "",
                val heightCm: Double = 0.0,
                val weightKg: Double = 0.0,
                val unitSystem: String = "METRIC",
                val activityLevel: String = "",
                val goal: String = "",
                val preferredDiet: String = "",
                val excludedIngredients: List<String> = emptyList(),
                val workoutPreference: String = ""
)
