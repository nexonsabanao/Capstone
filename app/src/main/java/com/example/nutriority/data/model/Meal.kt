package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String,
    val calories: Int,
    val category: String,
    val ingredients: List<String>,
    val time: String,
    val imageName: String
) {
    @Ignore
    var imageResId: Int = 0
}