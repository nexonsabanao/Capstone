// In Meal.kt
package com.example.nutriority.Models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val calories: String,
    val imageResId: Int,
    val category: String,
    val ingredients: List<String>,

    val time: String
)
