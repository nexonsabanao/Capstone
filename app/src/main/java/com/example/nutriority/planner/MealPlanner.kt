package com.example.nutriority.planner

import android.util.Log
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.repository.MealRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class MealPlanner @Inject constructor(
    private val mealRepository: MealRepository
) {

    // Main function to generate a meal plan based on user preferences.
    suspend fun planMeals(dailyCalories: Int, preferredDiet: String, excludedIngredients: List<String>): List<Meal> {
        val splits = listOf(0.30, 0.35, 0.35) // Breakfast, Lunch, Dinner percentages
        val targetCalories = splits.map { (dailyCalories * it).toInt() }

        val allMeals = mealRepository.getAllMealsList()

        if (allMeals.isEmpty()) {
            Log.w("MealPlanner", "The meal database is empty. Cannot generate a meal plan.")
            return emptyList()
        }

        // Filter meals based on dietary preferences and exclusions
        val filteredMeals = allMeals.filter { meal ->
            val isExcluded = excludedIngredients.any { ex -> meal.ingredients.any { it.contains(ex, true) } }
            if (isExcluded) return@filter false

            // Priority 1: Match the user's preferred diet if specified in the meal data
            if (meal.preferredDiet.isNotEmpty() && !meal.preferredDiet.equals("Balanced", true)) {
                if (!meal.preferredDiet.equals(preferredDiet, true)) return@filter false
            }

            // Priority 2: Fallback logic check if meal data lacks specific diet tagging
            when (preferredDiet.lowercase()) {
                "vegetarian" -> !containsMeat(meal)
                "low carb" -> isLowCarb(meal)
                else -> true 
            }
        }

        val mealTimes = listOf("Breakfast", "Lunch", "Dinner")
        val availableMeals = filteredMeals.toMutableList()
        val plannedMeals = mutableListOf<Meal>()

        mealTimes.zip(targetCalories).forEach { (time, targetCal) ->
            val bestMealsForTime = availableMeals
                .filter { it.mealTime.equals(time, ignoreCase = true) }
                .sortedBy { abs(it.calories.toDouble() - targetCal.toDouble()) } 
                .take(15) 

            if (bestMealsForTime.isNotEmpty()) {
                val chosenMeal = bestMealsForTime.random()
                // We keep the original calories but the planner picks the closest one
                plannedMeals.add(chosenMeal)
                availableMeals.remove(chosenMeal) 
            }
        }

        if (plannedMeals.size < 3) {
            Log.w("MealPlanner", "Incomplete plan: Found ${plannedMeals.size} meals for diet: $preferredDiet")
        }

        return plannedMeals
    }

    private fun containsMeat(meal: Meal): Boolean = meal.ingredients.any { it.contains("chicken", true) || it.contains("beef", true) || it.contains("pork", true) }
    private fun isLowCarb(meal: Meal): Boolean = !meal.ingredients.any { it.contains("bread", true) || it.contains("pasta", true) || it.contains("rice", true) || it.contains("potato", true) }
}
