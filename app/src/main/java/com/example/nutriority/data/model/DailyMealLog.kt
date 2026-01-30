package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_meal_logs")
data class DailyMealLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val mealId: Int,
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val time: String, // Breakfast, Lunch, Dinner, Snack
    val date: Long, // timestamp
    val imageName: String
)
