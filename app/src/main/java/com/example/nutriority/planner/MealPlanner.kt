package com.example.nutriority.planner

import android.util.Log
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.repository.MealRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class MealPlanner @Inject constructor(
    val mealRepository: MealRepository
) {

    /**
     * Optimized meal planning.
     * @param mealPool Optional pre-fetched list of all meals to avoid repeated DB hits.
     */
    suspend fun planMeals(
        dailyCalories: Int, 
        preferredDiet: String, 
        excludedIngredients: List<String>,
        mealPool: List<Meal>? = null
    ): List<Meal> {
        val splits = listOf(0.30, 0.35, 0.35) // Breakfast, Lunch, Dinner percentages
        val targetCalories = splits.map { (dailyCalories * it).toInt() }

        val allMeals = mealPool ?: mealRepository.getAllMealsList()

        if (allMeals.isEmpty()) {
            Log.w("MealPlanner", "The meal database is empty.")
            return emptyList()
        }

        // 1. Pre-filter by exclusions and diet once
        val filteredMeals = allMeals.filter { meal ->
            val isExcluded = excludedIngredients.any { ex -> meal.ingredients.any { it.contains(ex, true) } }
            if (isExcluded) return@filter false

            if (meal.preferredDiet.isNotEmpty() && !meal.preferredDiet.equals("Balanced", true)) {
                if (!meal.preferredDiet.equals(preferredDiet, true)) return@filter false
            }

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
                .sortedBy { abs(it.calories - targetCal) } 
                .take(15) 

            if (bestMealsForTime.isNotEmpty()) {
                val chosenMeal = bestMealsForTime.random()
                plannedMeals.add(chosenMeal)
                // Don't remove if we want potential duplicates across days, 
                // but keep it for within-day variety
                availableMeals.remove(chosenMeal) 
            }
        }

        return plannedMeals
    }

    private fun containsMeat(meal: Meal): Boolean = meal.ingredients.any { it.contains("chicken", true) || it.contains("beef", true) || it.contains("pork", true) }
    private fun isLowCarb(meal: Meal): Boolean = !meal.ingredients.any { it.contains("bread", true) || it.contains("pasta", true) || it.contains("rice", true) || it.contains("potato", true) }
}
