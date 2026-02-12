package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_meal_logs")
data class DailyMealLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val mealId: String, 
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val mealTime: String, 
    val date: Long, 
    val imageName: String,
    val firestoreId: String? = null
)
