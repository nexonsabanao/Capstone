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
     * Generates a full 7-day meal plan with variety.
     */
    suspend fun planWeek(
        dailyCalories: Int,
        preferredDiet: String,
        excludedIngredients: List<String>,
        mealPool: List<Meal>? = null
    ): List<List<Meal>> {
        val allMeals = mealPool ?: mealRepository.getAllMealsList()
        val usedMealIds = mutableSetOf<String>()
        val weekPlan = mutableListOf<List<Meal>>()

        repeat(7) {
            val dayMeals = planMeals(dailyCalories, preferredDiet, excludedIngredients, allMeals, usedMealIds)
            weekPlan.add(dayMeals)
            // track used meals to encourage variety across the week
            usedMealIds.addAll(dayMeals.map { it.id })
            
            // If we've used a lot of meals, we might want to allow some repeats if the pool is small
            // but for 7 days (21 meals), if the filtered pool has > 30 meals, it should be fine.
        }
        return weekPlan
    }

    /**
     * Optimized meal planning for a single day.
     * @param usedMealIds Optional set of IDs to avoid for variety.
     */
    suspend fun planMeals(
        dailyCalories: Int, 
        preferredDiet: String, 
        excludedIngredients: List<String>,
        mealPool: List<Meal>? = null,
        usedMealIds: Set<String> = emptySet()
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
            val isExcluded = excludedIngredients.any { excluded ->
                val normalizedExcluded = normalizeIngredient(excluded)
                meal.ingredients.any { ingredient ->
                    val normalizedIngredient = normalizeIngredient(ingredient)
                    normalizedIngredient.contains(normalizedExcluded, true) || 
                    normalizedExcluded.contains(normalizedIngredient, true)
                }
            }
            
            if (isExcluded) return@filter false

            if (preferredDiet.isNotBlank() && !preferredDiet.equals("Balanced", ignoreCase = true)) {
                if (meal.preferredDiet.isNotBlank() && 
                    !meal.preferredDiet.equals("Balanced", ignoreCase = true) && 
                    !meal.preferredDiet.equals(preferredDiet, ignoreCase = true)) {
                    return@filter false
                }
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
            val timeMatchingMeals = availableMeals.filter { it.mealTime.equals(time, ignoreCase = true) }
            
            if (timeMatchingMeals.isNotEmpty()) {
                // Prioritize variety: try to pick from meals not used yet in the week
                val unusedOptions = timeMatchingMeals.filter { !usedMealIds.contains(it.id) }
                
                // If we have enough unused options, pick from them. Otherwise, use all available for this time.
                val poolToPickFrom = if (unusedOptions.size >= 3) unusedOptions else timeMatchingMeals

                val bestMealsForTime = poolToPickFrom
                    .sortedBy { abs(it.calories - targetCal) } 
                    .take(10) // Take top 10 closest to target calories

                if (bestMealsForTime.isNotEmpty()) {
                    val chosenMeal = bestMealsForTime.random()
                    plannedMeals.add(chosenMeal)
                    // Remove from available so we don't pick the same meal twice in the SAME day
                    availableMeals.removeAll { it.id == chosenMeal.id }
                }
            }
        }

        return plannedMeals
    }

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
