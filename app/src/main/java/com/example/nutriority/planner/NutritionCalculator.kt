package com.example.nutriority.planner

import kotlin.math.roundToInt

/**
 * Nutrition calculator that computes BMR and TDEE using the Mifflin-St Jeor formula
 * and maps activity / goal to calorie targets.
 */
object NutritionCalculator {

    fun calculateBmr(weightKg: Double, heightCm: Double, age: Int, gender: String): Double {
        // Mifflin-St Jeor
        val s = if (gender.equals("Male", true) || gender.equals("M", true)) 5 else -161
        return 10.0 * weightKg + 6.25 * heightCm - 5.0 * age + s
    }

    fun activityFactor(activityLevel: String): Double = when (activityLevel) {
        "Sedentary" -> 1.2
        "Lightly Active" -> 1.375
        "Active" -> 1.55
        else -> 1.3
    }

    fun goalAdjustmentFactor(goal: String): Double = when (goal.lowercase()) {
        "lose weight", "weight loss" -> 0.80 // reduce calories by ~20%
        "build muscle" -> 1.15 // increase calories by ~15%
        "keep fit" -> 1.0
        else -> 1.0
    }

    fun calculateTdeeDailyCalories(weightKg: Double, heightCm: Double, age: Int, gender: String, activityLevel: String, goal: String): Int {
        val bmr = calculateBmr(weightKg, heightCm, age, gender)
        val tdee = bmr * activityFactor(activityLevel) * goalAdjustmentFactor(goal)
        return tdee.roundToInt()
    }

    fun getCalorieRangeForDisplay(weightKg: Double, heightCm: Double, age: Int, gender: String, activityLevel: String, goal: String): String {
        val dailyCalories = calculateTdeeDailyCalories(weightKg, heightCm, age, gender, activityLevel, goal)
        val lowerBound = (dailyCalories * 0.9)
        val upperBound = (dailyCalories * 1.1)

        // Round to the nearest 100
        val roundedLower = (lowerBound / 100.0).roundToInt() * 100
        val roundedUpper = (upperBound / 100.0).roundToInt() * 100

        return "$roundedLower-$roundedUpper kcal / day"
    }

    fun macronutrientTargets(dailyCalories: Int, proteinPercent: Double = 0.25, carbPercent: Double = 0.45, fatPercent: Double = 0.30): MacroTargets {
        // Protein: 4 kcal/g, Carbs: 4 kcal/g, Fat: 9 kcal/g
        val proteinGrams = ((dailyCalories * proteinPercent) / 4.0).roundToInt()
        val carbsGrams = ((dailyCalories * carbPercent) / 4.0).roundToInt()
        val fatGrams = ((dailyCalories * fatPercent) / 9.0).roundToInt()

        return MacroTargets(proteinGrams, carbsGrams, fatGrams)
    }
}
