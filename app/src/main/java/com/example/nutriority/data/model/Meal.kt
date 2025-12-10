package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val calories: String,
    val category: String,
    val ingredients: List<String>, // Your converter handles this perfectly.
    val time: String,
    val imageName: String
) {
    @Ignore
    var imageResId: Int = 0
}