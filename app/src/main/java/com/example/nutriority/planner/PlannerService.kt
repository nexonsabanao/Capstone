package com.example.nutriority.planner

import com.example.nutriority.data.model.User

/**
 * PlannerService ties together the nutrition and workout planners and exposes a single
 * method to generate a PersonalizedPlan from a given User.
 */
object PlannerService {

    fun generatePlanForUser(user: User): PersonalizedPlan {
        val input = PlannerInput(user, user.age ?: 30)

        val dailyCalories = NutritionCalculator.calculateTdeeDailyCalories(
            weightKg = user.weightKg,
            heightCm = user.heightCm,
            age = input.age,
            gender = user.gender,
            activityLevel = user.activityLevel,
            goal = user.goal
        )

        val macros = NutritionCalculator.macronutrientTargets(dailyCalories)

        val meals = MealPlanner.planMeals(dailyCalories, user.preferredDiet, user.excludedIngredients)

        val nutritionPlan = NutritionPlan(dailyCalories, macros, meals)

        val workoutPlan = WorkoutPlanner.planWorkouts(user.goal)

        val plan = PersonalizedPlan(user.id, nutritionPlan, workoutPlan)

        return plan
    }

    fun planSummary(plan: PersonalizedPlan): String {
        val sb = StringBuilder()
        sb.append("Daily Calories: ${plan.nutritionPlan.dailyCalories} kcal\n")
        sb.append("Macros: P ${plan.nutritionPlan.macroTargets.proteinGrams}g | C ${plan.nutritionPlan.macroTargets.carbsGrams}g | F ${plan.nutritionPlan.macroTargets.fatGrams}g\n")
        sb.append("Meals:\n")
        plan.nutritionPlan.meals.forEach { m -> sb.append(" - ${m.name} (${m.calories} kcal) - ${m.description}\n") }
        sb.append("\nWorkout Plan (weekly burn estimate ${plan.workoutPlan.weeklyCaloriesBurn} kcal):\n")
        plan.workoutPlan.sessions.forEach { s -> sb.append(" - ${s.day}: ${s.focus} (${s.durationMinutes} min)\n") }

        return sb.toString()
    }
}
