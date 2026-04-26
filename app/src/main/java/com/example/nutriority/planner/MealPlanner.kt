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

    /**
     * Generates a full 7-day meal plan with variety and macro-awareness.
     */
    suspend fun planWeek(
        macroTarget: DailyMacroTarget,
        preferredDiet: String,
        excludedIngredients: List<String>,
        mealPool: List<Meal>? = null
    ): List<List<Meal>> {
        val allMeals = mealPool ?: mealRepository.getAllMealsList()
        val usedMealIds = mutableSetOf<String>()
        val weekPlan = mutableListOf<List<Meal>>()

        repeat(7) {
            val dayMeals = planMealsSmart(macroTarget, preferredDiet, excludedIngredients, allMeals, usedMealIds)
            weekPlan.add(dayMeals)
            // track used meals to encourage variety across the week
            usedMealIds.addAll(dayMeals.map { it.id })
            
            // Allow some repetition if we've gone through a lot of the database
            if (usedMealIds.size > (allMeals.size * 0.7)) {
                usedMealIds.clear()
            }
        }
        return weekPlan
    }

    /**
     * Optimized macro-aware meal planning for a single day.
     */
    suspend fun planMealsSmart(
        target: DailyMacroTarget,
        preferredDiet: String,
        excludedIngredients: List<String>,
        mealPool: List<Meal>? = null,
        usedMealIds: Set<String> = emptySet()
    ): List<Meal> {
        val allMeals = mealPool ?: mealRepository.getAllMealsList()

        if (allMeals.isEmpty()) {
            Log.w("MealPlanner", "The meal database is empty.")
            return emptyList()
        }

        // 1. Filter by diet and exclusions
        val filteredMeals = allMeals.filter { meal ->
            isMealAllowed(meal, preferredDiet, excludedIngredients)
        }

        // 2. Define meal distribution (Breakfast, Lunch, Dinner)
        val mealConfig = listOf(
            Triple("Breakfast", 0.30, 0.30), // Time, Calorie Ratio, Macro Ratio
            Triple("Lunch", 0.35, 0.35),
            Triple("Dinner", 0.35, 0.35)
        )

        val selectedMeals = mutableListOf<Meal>()
        val availablePool = filteredMeals.toMutableList()

        for ((time, calRatio, macroRatio) in mealConfig) {
            val targetCals = (target.calories * calRatio).toInt()
            val targetProtein = (target.protein * macroRatio)
            val targetCarbs = (target.carbs * macroRatio)
            val targetFat = (target.fat * macroRatio)

            val candidates = availablePool.filter { it.mealTime.equals(time, true) }
            
            if (candidates.isNotEmpty()) {
                // Prioritize unused meals
                val unusedCandidates = candidates.filter { !usedMealIds.contains(it.id) }
                val currentPool = if (unusedCandidates.size >= 2) unusedCandidates else candidates

                // Score meals based on calorie and full macro proximity
                val bestMeal = currentPool.minByOrNull { meal ->
                    scoreMeal(meal, targetCals, targetProtein, targetCarbs, targetFat)
                }

                if (bestMeal != null) {
                    selectedMeals.add(bestMeal)
                    availablePool.removeAll { it.id == bestMeal.id }
                }
            }
        }

        // Fallback: If we missed a meal time, fill it without time restriction
        if (selectedMeals.size < 3 && availablePool.isNotEmpty()) {
            val missingCount = 3 - selectedMeals.size
            repeat(missingCount) {
                val fallback = availablePool.minByOrNull { scoreMeal(it, (target.calories * 0.33).toInt(), (target.protein * 0.33), (target.carbs * 0.33), (target.fat * 0.33)) }
                fallback?.let { 
                    selectedMeals.add(it)
                    availablePool.remove(it)
                }
            }
        }

        return selectedMeals
    }

    private fun scoreMeal(meal: Meal, targetCals: Int, targetProtein: Double, targetCarbs: Double, targetFat: Double): Double {
        val calorieDiff = abs(meal.calories - targetCals).toDouble()
        // Protein is weighted highest (x2.0)
        val proteinDiff = abs(meal.macros.protein - targetProtein) * 2.0
        // Carbs and Fats are weighted normally (x1.0)
        val carbDiff = abs(meal.macros.carbs - targetCarbs) * 1.0
        val fatDiff = abs(meal.macros.fats - targetFat) * 1.0
        
        return calorieDiff + proteinDiff + carbDiff + fatDiff
    }

    private fun isMealAllowed(meal: Meal, diet: String, exclusions: List<String>): Boolean {
        // Check exclusions
        val hasExclusion = exclusions.any { excluded ->
            val normEx = excluded.trim().lowercase()
            meal.ingredients.any { it.contains(normEx, true) }
        }
        if (hasExclusion) return false

        // Check diet
        return when (diet.lowercase()) {
            "vegetarian" -> !containsMeat(meal)
            "low-carb", "low carb" -> isLowCarb(meal)
            "vegan" -> !containsAnimalProducts(meal)
            else -> true
        }
    }

    private fun containsMeat(meal: Meal): Boolean {
        return meal.ingredients.any { ing ->
            meatKeywords.any { keyword -> ing.contains(keyword, true) }
        }
    }

    private fun isLowCarb(meal: Meal): Boolean {
        // Checks if meal explicitly labeled low carb OR ingredients are safe
        if (meal.preferredDiet.contains("Low Carb", true)) return true
        
        val hasHeavyCarbs = meal.ingredients.any { ing ->
            carbHeavyKeywords.any { keyword -> ing.contains(keyword, true) }
        }
        return !hasHeavyCarbs
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
