package com.example.nutriority.planner

import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.model.User
import com.google.gson.annotations.SerializedName

/**
 * Lightweight domain models for the planner system.
 */
data class NutritionPlan(
    val dailyCalories: Int,
    val macroTargets: MacroTargets,
    val meals: List<Meal>
)

data class MacroTargets(
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int
)

data class WorkoutPlan(
    val weeklyCaloriesBurn: Int,
    val sessions: List<WorkoutSession>
)

data class WorkoutSession(
    val day: String,
    val durationMinutes: Int,
    val focus: String,
    val description: String,
    val caloriesBurned: Int = 0, // Added for daily calorie calculation
    @SerializedName("workout") val workoutDetails: WorkoutDetails? = null,
    // This field is for backward compatibility with old JSON schemas.
    private val workoutId: Int? = null,
    val sets: Int? = null, 
    val reps: String? = null 
) {
    // This computed property provides a unified way to access the workout ID.
    // It is not serialized and is safe from reflection issues with Gson.
    val unifiedWorkoutId: Int?
        get() = workoutDetails?.id ?: workoutId
}


data class WorkoutDetails(
    val id: Int,
    val sets: Int? = null, 
    val reps: String? = null 
)

// Simple holder for combined plan
data class PersonalizedPlan(
    val userId: Int,
    val nutritionPlan: NutritionPlan,
    val workoutPlan: WorkoutPlan
)

/** helper: lightweight input model derived from User for planner logic */
data class PlannerInput(
    val user: User,
    val age: Int = 30 // default when not available
)
