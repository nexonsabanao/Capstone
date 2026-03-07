package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.nutriority.data.local.Converters
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "user_profile")
@TypeConverters(Converters::class)
data class User(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1,
    var email: String = "",
    var name: String = "",
    var profileImageUrl: String = "",
    var gender: String = "",
    var birthDate: Long? = null,
    var heightCm: Double = 0.0,
    var weightKg: Double = 0.0,
    var unitSystem: String = "METRIC",
    var activityLevel: String = "",
    var goal: String = "",
    var preferredDiet: String = "",
    var excludedIngredients: List<String> = emptyList(),
    var personalizedPlanJson: String? = null,
    var mealPlanJson: String? = null,
    var lastCompletedWorkoutDay: Int = 0,
    var status: String = "active", // Added status field
    
    // Progress Stats
    var totalCaloriesBurned: Int = 0,
    var totalWorkoutMinutes: Long = 0,
    var totalWorkoutsCompleted: Int = 0
)
