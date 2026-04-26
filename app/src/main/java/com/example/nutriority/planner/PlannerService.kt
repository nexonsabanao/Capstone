package com.example.nutriority.planner

import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.Meal
import com.example.nutriority.ui.util.AgeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannerService @Inject constructor(
    val mealPlanner: MealPlanner,
    val workoutPlanner: WorkoutPlanner
) {

    /**
     * Generates a full personalized plan including a 4-week workout program
     * and a full 7-day meal plan based on the user's current metrics.
     */
    suspend fun generateFullPlan(user: User): Pair<WorkoutPlan, List<List<Meal>>> = withContext(Dispatchers.IO) {
        // 1. Generate 4-week workout plan
        val workoutPlanDeferred = async { workoutPlanner.planWorkouts(user) }

        // 2. Calculate nutritional targets
        val age = AgeUtil.calculateAge(user.birthDate)
        val dailyCalories = NutritionCalculator.calculateTdeeDailyCalories(
            user.weightKg, user.heightCm, age, user.gender, user.activityLevel, user.goal
        )
        val macros = NutritionCalculator.macronutrientTargets(dailyCalories, user.weightKg, user.goal)
        
        val dailyTarget = DailyMacroTarget(
            calories = dailyCalories,
            protein = macros.proteinGrams,
            carbs = macros.carbsGrams,
            fat = macros.fatGrams
        )

        // 3. Generate 7-day meal plan
        val mealPlan = mealPlanner.planWeek(
            dailyTarget, 
            user.preferredDiet, 
            user.excludedIngredients
        )

        val workoutPlan = workoutPlanDeferred.await()

        workoutPlan to mealPlan
    }

    /**
     * Calculates the daily calorie target for the user.
     */
    fun calculateDailyTarget(user: User): Int {
        return NutritionCalculator.calculateTdeeDailyCalories(
            user.weightKg, user.heightCm, AgeUtil.calculateAge(user.birthDate), user.gender, user.activityLevel, user.goal
        )
    }

    /**
     * Calculates the macronutrient targets for the user.
     */
    fun calculateMacroTargets(calories: Int, user: User): MacroTargets {
        return NutritionCalculator.macronutrientTargets(calories, user.weightKg, user.goal)
    }
}
