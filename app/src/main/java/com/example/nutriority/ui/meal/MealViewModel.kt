package com.example.nutriority.ui.meal

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.planner.MealPlanner
import com.example.nutriority.planner.NutritionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository,
    private val mealPlanner: MealPlanner
) : ViewModel() {

    val allMeals: LiveData<List<Meal>> = mealRepository.allMeals.asLiveData()

    private val _mealPlan = MutableLiveData<List<List<Meal>>>()
    val mealPlan: LiveData<List<List<Meal>>> = _mealPlan

    fun generateMealPlan() {
        viewModelScope.launch {
            val user = userRepository.getInitialUser()
            if (user != null) {
                val dailyCalories = NutritionCalculator.calculateTdeeDailyCalories(
                    user.weightKg,
                    user.heightCm,
                    user.age ?: 30,
                    user.gender,
                    user.activityLevel,
                    user.goal
                )
                
                val weeklyPlan = (1..7).map { 
                    mealPlanner.planMeals(dailyCalories, user.preferredDiet, user.excludedIngredients)
                }
                _mealPlan.postValue(weeklyPlan)

            } else {
                Log.w("MealViewModel", "User not found, generating default meal plan.")
                val weeklyPlan = (1..7).map { 
                    mealPlanner.planMeals(2000, "Balanced", emptyList())
                }
                _mealPlan.postValue(weeklyPlan)
            }
        }
    }
}
