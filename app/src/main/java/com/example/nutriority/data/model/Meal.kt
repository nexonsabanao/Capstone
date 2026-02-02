package com.example.nutriority.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val instructions: String = "",
    val calories: Int = 0,
    val category: String = "",
    val ingredients: List<String> = emptyList(),
    val mealTime: String = "",
    val imageName: String = "",
    val duration: Int = 0,
    val preferredDiet: String = "",
    @Embedded
    val macros: Macros = Macros()
) {
    @Ignore
    var imageResId: Int = 0
}

data class Macros(
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0
)
