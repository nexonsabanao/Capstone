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

        // Early exit if the database contains no meals at all.
        if (allMeals.isEmpty()) {
            Log.w("MealPlanner", "The meal database is empty. Cannot generate a meal plan.")
            return emptyList()
        }

        // Filter meals based on the user's dietary restrictions and preferences.
        val filteredMeals = allMeals.filter { meal ->
            val isExcluded = excludedIngredients.any { ex -> meal.ingredients.any { it.contains(ex, true) } }
            if (isExcluded) {
                return@filter false
            }

            when (preferredDiet.lowercase()) {
                "vegetarian" -> !containsMeat(meal)
                "low-carb" -> isLowCarb(meal)
                "vegan" -> !containsAnimalProducts(meal)
                else -> true // For "Balanced" diet, no special filtering is needed.
            }
        }

        // For each meal slot (breakfast, lunch, dinner), find the best-matching meal from the filtered list.
        // If no suitable meal is found for a slot (e.g., filtered list is empty), mapNotNull will safely discard it.
        val plannedMeals = targetCalories.mapNotNull { targetCal ->
            val bestMeal = filteredMeals.minByOrNull { abs(it.calories.toDouble() - targetCal.toDouble()) }
            bestMeal?.copy(calories = targetCal)
        }

        // Log a warning if a full 3-meal plan could not be generated.
        if (plannedMeals.size < 3) {
            Log.w("MealPlanner", "Could not find suitable meals for all slots for the diet: '$preferredDiet'. Found ${plannedMeals.size} meals.")
        }

        return plannedMeals
    }

    private fun containsMeat(meal: Meal): Boolean = meal.ingredients.any { it.contains("chicken", true) || it.contains("beef", true) || it.contains("pork", true) }
    private fun isLowCarb(meal: Meal): Boolean = !meal.ingredients.any { it.contains("bread", true) || it.contains("pasta", true) || it.contains("rice", true) || it.contains("potato", true) }
    private fun containsAnimalProducts(meal: Meal): Boolean = meal.ingredients.any { ing -> listOf("chicken","beef","pork","salmon","yogurt","cheese","egg","milk","honey").any { ing.contains(it, true) } }
}