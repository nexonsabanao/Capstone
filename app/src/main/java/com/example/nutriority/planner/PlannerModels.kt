package com.example.nutriority.planner

import com.example.nutriority.data.User

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

data class Meal(
    val name: String,
    val calories: Int,
    val description: String,
    val ingredients: List<String>,
    val imageName: String = ""
)

data class WorkoutPlan(
    val weeklyCaloriesBurn: Int,
    val sessions: List<WorkoutSession>
)

data class WorkoutSession(
    val day: String,
    val durationMinutes: Int,
    val focus: String,
    val description: String
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
