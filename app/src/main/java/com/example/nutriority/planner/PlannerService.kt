package com.example.nutriority.planner

import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.Meal
import com.example.nutriority.ui.util.AgeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannerService @Inject constructor(
    val mealPlanner: MealPlanner,
    val workoutPlanner: WorkoutPlanner
) {

    /**
     * Generates a full personalized plan including a 4-week workout program
     * and a full 7-day meal plan.
     */
    suspend fun generateFullPlan(user: User): Pair<WorkoutPlan, List<List<Meal>>> = withContext(Dispatchers.IO) {
        // 1. Generate 4-week workout plan
        val workoutPlanDeferred = async { workoutPlanner.planWorkouts(user) }

        // 2. Generate 7-day meal plan in parallel
        val dailyCalories = NutritionCalculator.calculateTdeeDailyCalories(
            user.weightKg, user.heightCm, AgeUtil.calculateAge(user.birthDate), user.gender, user.activityLevel, user.goal
        )
        
        // Fetch meal pool once to optimize speed
        val mealPool = mealPlanner.mealRepository.getAllMealsList()
        
        val mealPlanDeferred = (0 until 7).map {
            async { 
                mealPlanner.planMeals(dailyCalories, user.preferredDiet, user.excludedIngredients, mealPool) 
            }
        }

        val workoutPlan = workoutPlanDeferred.await()
        val mealPlan = mealPlanDeferred.awaitAll()

        workoutPlan to mealPlan
    }

    suspend fun generatePlanForUser(user: User): PersonalizedPlan {
        val age = AgeUtil.calculateAge(user.birthDate)
        val input = PlannerInput(user, age)

        val dailyCalories = NutritionCalculator.calculateTdeeDailyCalories(
            weightKg = user.weightKg,
            heightCm = user.heightCm,
            age = input.age,
            gender = user.gender,
            activityLevel = user.activityLevel,
            goal = user.goal
        )

        val macros = NutritionCalculator.macronutrientTargets(dailyCalories)
        val meals: List<Meal> = mealPlanner.planMeals(dailyCalories, user.preferredDiet, user.excludedIngredients)
        val nutritionPlan = NutritionPlan(dailyCalories, macros, meals)
        val workoutPlan = workoutPlanner.planWorkouts(user)

        return PersonalizedPlan(user.id, nutritionPlan, workoutPlan)
    }

    fun planSummary(plan: PersonalizedPlan): String {
        val sb = StringBuilder()
        sb.append("Daily Calories: ${plan.nutritionPlan.dailyCalories} kcal\n")
        sb.append("Macros: P ${plan.nutritionPlan.macroTargets.proteinGrams}g | C ${plan.nutritionPlan.macroTargets.carbsGrams}g | F ${plan.nutritionPlan.macroTargets.fatGrams}g\n")
        sb.append("Meals:\n")
        plan.nutritionPlan.meals.forEach { m -> sb.append(" - ${m.name} (${m.calories} kcal) - ${m.mealTime}\n") }
        sb.append("\nWorkout Plan (weekly burn estimate ${plan.workoutPlan.weeklyCaloriesBurn} kcal):\n")
        plan.workoutPlan.sessions.forEach { s -> sb.append(" - ${s.day}: ${s.focus} (${s.durationMinutes} min)\n") }

        return sb.toString()
    }
}
