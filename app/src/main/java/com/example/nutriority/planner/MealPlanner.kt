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
            // Improved exclusion logic to handle plurals like "Egg" vs "Eggs"
            val isExcluded = excludedIngredients.any { excluded ->
                val normalizedExcluded = normalizeIngredient(excluded)
                meal.ingredients.any { ingredient ->
                    val normalizedIngredient = normalizeIngredient(ingredient)
                    normalizedIngredient.contains(normalizedExcluded, true) || 
                    normalizedExcluded.contains(normalizedIngredient, true)
                }
            }
            
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
                availableMeals.remove(chosenMeal) 
            }
        }

        return plannedMeals
    }

    /**
     * Basic normalization to handle plurals and casing.
     * Converts to lowercase and strips trailing 's'.
     */
    private fun normalizeIngredient(input: String): String {
        val lower = input.lowercase().trim()
        return if (lower.endsWith("s") && lower.length > 3) {
            lower.substring(0, lower.length - 1)
        } else {
            lower
        }
    }

    private fun containsMeat(meal: Meal): Boolean = meal.ingredients.any { 
        val norm = normalizeIngredient(it)
        norm.contains("chicken") || norm.contains("beef") || norm.contains("pork") 
    }
    
    private fun isLowCarb(meal: Meal): Boolean = !meal.ingredients.any { 
        val norm = normalizeIngredient(it)
        norm.contains("bread") || norm.contains("pasta") || norm.contains("rice") || norm.contains("potato") 
    }
}
