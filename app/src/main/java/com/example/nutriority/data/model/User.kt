package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.nutriority.data.local.Converters

@Entity(tableName = "user_profile")
@TypeConverters(Converters::class)
data class User(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1,
    val gender: String = "",
    val age: Int? = null,
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val unitSystem: String = "METRIC",
    val activityLevel: String = "",
    val goal: String = "",
    val preferredDiet: String = "",
    val excludedIngredients: List<String> = emptyList()
)