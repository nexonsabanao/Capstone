package com.example.nutriority.planner

import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.model.User
import com.google.gson.annotations.SerializedName

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
    val caloriesBurned: Int = 0,
    @SerializedName("workout") val workoutDetails: WorkoutDetails? = null,
    val warmup: List<PlannerExercise>? = null,
    val exercises: List<PlannerExercise>? = null,
    val cooldown: List<PlannerExercise>? = null
) {
    val unifiedWorkoutId: Int?
        get() = workoutDetails?.id
}

data class WorkoutDetails(
    val id: Int
)

data class PlannerExercise(
    val name: String,
    val exerciseId: String,
    val sets: Int,
    val reps: String,
    val duration: String,
    val rest: String
)

data class PersonalizedPlan(
    val userId: Int,
    val nutritionPlan: NutritionPlan,
    val workoutPlan: WorkoutPlan
)

data class PlannerInput(
    val user: User,
    val age: Int = 30 
)
