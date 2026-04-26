package com.example.nutriority.planner

import kotlin.math.roundToInt

/**
 * Nutrition calculator that computes BMR and TDEE using the Mifflin-St Jeor formula
 * and maps activity / goal to calorie targets with body-weight based macro distribution.
 */
object NutritionCalculator {

    enum class Gender { MALE, FEMALE }
    enum class Goal { LOSE_WEIGHT, KEEP_FIT, BUILD_MUSCLE }
    enum class ActivityLevel(val multiplier: Double) {
        SEDENTARY(1.2),
        LIGHTLY_ACTIVE(1.375),
        MODERATE(1.55),
        ACTIVE(1.725),
        VERY_ACTIVE(1.9)
    }

    private fun parseGender(gender: String): Gender {
        return if (gender.contains("Female", true) || gender.equals("F", true)) Gender.FEMALE else Gender.MALE
    }

    private fun parseGoal(goal: String): Goal = when (goal.lowercase()) {
        "lose weight", "weight loss", "cut" -> Goal.LOSE_WEIGHT
        "build muscle", "bulk" -> Goal.BUILD_MUSCLE
        else -> Goal.KEEP_FIT
    }

    private fun parseActivity(level: String): ActivityLevel = when (level.lowercase()) {
        "sedentary" -> ActivityLevel.SEDENTARY
        "lightly active", "light" -> ActivityLevel.LIGHTLY_ACTIVE
        "active", "moderate" -> ActivityLevel.MODERATE
        "very active" -> ActivityLevel.ACTIVE
        else -> ActivityLevel.LIGHTLY_ACTIVE
    }

    /**
     * Base Metabolism Rate (BMR) - Calories your body burns at rest.
     */
    fun calculateBmr(weightKg: Double, heightCm: Double, age: Int, genderStr: String): Double {
        val gender = parseGender(genderStr)
        val s = if (gender == Gender.MALE) 5 else -161
        return (10 * weightKg) + (6.25 * heightCm) - (5 * age) + s
    }

    /**
     * Total Daily Energy Expenditure (TDEE) - BMR adjusted for activity and fitness goal.
     */
    fun calculateTdeeDailyCalories(weightKg: Double, heightCm: Double, age: Int, gender: String, activityLevel: String, goal: String): Int {
        val bmr = calculateBmr(weightKg, heightCm, age, gender)
        val activity = parseActivity(activityLevel)
        val tdee = bmr * activity.multiplier

        // Adjust calories based on goal
        val targetCalories = when (parseGoal(goal)) {
            Goal.LOSE_WEIGHT -> (tdee - 400) // Fat loss deficit
            Goal.KEEP_FIT -> tdee          // Maintenance
            Goal.BUILD_MUSCLE -> (tdee + 300) // Lean bulk surplus
        }

        return targetCalories.roundToInt().coerceAtLeast(1200) // Ensure a safe minimum
    }

    fun getCalorieRangeForDisplay(weightKg: Double, heightCm: Double, age: Int, gender: String, activityLevel: String, goal: String): String {
        val dailyCalories = calculateTdeeDailyCalories(weightKg, heightCm, age, gender, activityLevel, goal)
        val lowerBound = (dailyCalories * 0.95)
        val upperBound = (dailyCalories * 1.05)

        // Round to the nearest 50 for a professional look
        val roundedLower = (lowerBound / 50.0).roundToInt() * 50
        val roundedUpper = (upperBound / 50.0).roundToInt() * 50

        return "$roundedLower-$roundedUpper kcal / day"
    }

    /**
     * Smart Macro Calculation:
     * Protein: based on body weight (1.6g - 2.2g per kg)
     * Fat: based on % of total calories (20% - 30%)
     * Carbs: the remaining calories
     */
    fun macronutrientTargets(dailyCalories: Int, weightKg: Double, goalStr: String): MacroTargets {
        val goal = parseGoal(goalStr)

        // 1. Protein Target
        val proteinPerKg = when (goal) {
            Goal.LOSE_WEIGHT -> 2.2  // Higher protein to preserve muscle during cut
            Goal.KEEP_FIT -> 1.8
            Goal.BUILD_MUSCLE -> 1.6 // Surplus helps protein synthesis
        }
        val proteinGrams = (weightKg * proteinPerKg).roundToInt()
        val proteinCalories = proteinGrams * 4

        // 2. Fat Target
        val fatPercent = when (goal) {
            Goal.LOSE_WEIGHT -> 0.25
            Goal.KEEP_FIT -> 0.30
            Goal.BUILD_MUSCLE -> 0.25
        }
        val fatCalories = dailyCalories * fatPercent
        val fatGrams = (fatCalories / 9.0).roundToInt()

        // 3. Carbs (Remaining Calories)
        val remainingCalories = dailyCalories - (proteinCalories + fatCalories)
        val carbsGrams = (remainingCalories / 4.0).roundToInt().coerceAtLeast(50)

        return MacroTargets(proteinGrams, carbsGrams, fatGrams)
    }

    // Overload for backward compatibility with existing calls
    fun macronutrientTargets(dailyCalories: Int, proteinPercent: Double, carbPercent: Double, fatPercent: Double): MacroTargets {
        val proteinGrams = ((dailyCalories * proteinPercent) / 4.0).roundToInt()
        val carbsGrams = ((dailyCalories * carbPercent) / 4.0).roundToInt()
        val fatGrams = ((dailyCalories * fatPercent) / 9.0).roundToInt()
        return MacroTargets(proteinGrams, carbsGrams, fatGrams)
    }
}
