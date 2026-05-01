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

    private val meatKeywords = listOf(
        "chicken", "beef", "pork", "lamb", "fish", "tuna", "salmon", "bacon", "shrimp", "steak", "meat"
    )

    private val carbHeavyKeywords = listOf(
        "bread", "pasta", "rice", "potato", "noodle", "dough", "flour", "tortilla"
    )

    suspend fun planWeek(
        macroTarget: DailyMacroTarget,
        preferredDiet: String,
        excludedIngredients: List<String>,
        mealPool: List<Meal>? = null
    ): List<List<Meal>> {
        val allMeals = mealPool ?: mealRepository.getAllMealsList()
        val usedMealIds = mutableSetOf<String>()
        val weekPlan = mutableListOf<List<Meal>>()

        repeat(7) { dayIndex ->
            val dayMeals = planMealsSmart(macroTarget, preferredDiet, excludedIngredients, allMeals, usedMealIds)
            weekPlan.add(dayMeals)
            usedMealIds.addAll(dayMeals.map { it.id })
            
            if (usedMealIds.size > (allMeals.size * 0.7)) {
                usedMealIds.clear()
            }
        }
        return weekPlan
    }

    /**
     * Optimized macro-aware meal planning that dynamically adjusts targets 
     * based on previously selected meals for the day.
     */
    suspend fun planMealsSmart(
        target: DailyMacroTarget,
        preferredDiet: String,
        excludedIngredients: List<String>,
        mealPool: List<Meal>? = null,
        usedMealIds: Set<String> = emptySet()
    ): List<Meal> {
        val allMeals = mealPool ?: mealRepository.getAllMealsList()

        if (allMeals.isEmpty()) return emptyList()

        val filteredMeals = allMeals.filter { meal ->
            isMealAllowed(meal, preferredDiet, excludedIngredients)
        }

        if (filteredMeals.isEmpty()) return emptyList()

        val selectedMeals = mutableListOf<Meal>()
        val availablePool = filteredMeals.toMutableList()

        // Track remaining needs for the day to adjust targets dynamically
        var remainingCals = target.calories.toDouble()
        var remainingProtein = target.protein.toDouble()
        var remainingCarbs = target.carbs.toDouble()
        var remainingFat = target.fat.toDouble()

        val mealTimes = listOf("Breakfast", "Lunch", "Dinner")

        for (i in mealTimes.indices) {
            val time = mealTimes[i]
            val mealsRemaining = 3 - i
            
            // Distribute remaining macros among remaining meals
            val targetCals = (remainingCals / mealsRemaining).toInt()
            val targetProtein = remainingProtein / mealsRemaining
            val targetCarbs = remainingCarbs / mealsRemaining
            val targetFat = remainingFat / mealsRemaining

            val candidates = availablePool.filter { it.mealTime.equals(time, true) }
            
            if (candidates.isNotEmpty()) {
                val unusedCandidates = candidates.filter { !usedMealIds.contains(it.id) }
                val currentPool = if (unusedCandidates.size >= 2) unusedCandidates else candidates

                val bestMeal = currentPool.shuffled()
                    .map { it to scoreMeal(it, targetCals, targetProtein, targetCarbs, targetFat) }
                    .sortedBy { it.second }
                    .take(5)
                    .randomOrNull()?.first

                if (bestMeal != null) {
                    selectedMeals.add(bestMeal)
                    availablePool.removeAll { it.id == bestMeal.id }
                    
                    // Subtract selected meal from remaining daily target
                    remainingCals -= bestMeal.calories
                    remainingProtein -= bestMeal.macros.protein
                    remainingCarbs -= bestMeal.macros.carbs
                    remainingFat -= bestMeal.macros.fats
                }
            }
        }

        // Fallback for missing slots
        if (selectedMeals.size < 3 && availablePool.isNotEmpty()) {
            val missingCount = 3 - selectedMeals.size
            repeat(missingCount) {
                val mealsRemaining = 3 - selectedMeals.size
                if (mealsRemaining <= 0) return@repeat

                val fallback = availablePool.shuffled()
                    .map { it to scoreMeal(it, (remainingCals / mealsRemaining).toInt(), remainingProtein / mealsRemaining, remainingCarbs / mealsRemaining, remainingFat / mealsRemaining) }
                    .sortedBy { it.second }
                    .take(5)
                    .randomOrNull()?.first
                    
                fallback?.let { 
                    selectedMeals.add(it)
                    availablePool.remove(it)
                    remainingCals -= it.calories
                }
            }
        }

        return selectedMeals
    }

    private fun scoreMeal(meal: Meal, targetCals: Int, targetProtein: Double, targetCarbs: Double, targetFat: Double): Double {
        val calorieDiff = abs(meal.calories - targetCals).toDouble()
        val proteinDiff = abs(meal.macros.protein - targetProtein) * 2.0
        val carbDiff = abs(meal.macros.carbs - targetCarbs) * 1.0
        val fatDiff = abs(meal.macros.fats - targetFat) * 1.0
        
        return calorieDiff + proteinDiff + carbDiff + fatDiff
    }

    private fun isMealAllowed(meal: Meal, diet: String, exclusions: List<String>): Boolean {
        val hasExclusion = exclusions.any { excluded ->
            val normEx = excluded.trim().lowercase()
            if (normEx.isBlank()) return@any false
            meal.ingredients.any { it.contains(normEx, true) }
        }
        if (hasExclusion) return false

        return when (diet.lowercase()) {
            "vegetarian" -> !containsMeat(meal)
            "low-carb", "low carb" -> isLowCarb(meal)
            "vegan" -> !containsAnimalProducts(meal)
            else -> true
        }
    }

    private fun containsMeat(meal: Meal): Boolean = meal.ingredients.any { ing ->
        meatKeywords.any { keyword -> ing.contains(keyword, true) }
    }

    private fun isLowCarb(meal: Meal): Boolean {
        if (meal.preferredDiet.contains("Low Carb", true)) return true
        return !meal.ingredients.any { ing ->
            carbHeavyKeywords.any { keyword -> ing.contains(keyword, true) }
        }
    }

    private fun containsAnimalProducts(meal: Meal): Boolean {
        val animalProducts = meatKeywords + listOf("egg", "milk", "cheese", "dairy", "honey", "yogurt", "butter")
        return meal.ingredients.any { ing ->
            animalProducts.any { keyword -> ing.contains(keyword, true) }
        }
    }
}

data class DailyMacroTarget(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)
