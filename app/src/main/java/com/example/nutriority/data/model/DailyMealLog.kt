package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_meal_logs")
data class DailyMealLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val mealId: String, // Changed to String to match Meal.id
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val mealTime: String, // Renamed from time to match Firestore
    val date: Long, // timestamp
    val imageName: String
)
