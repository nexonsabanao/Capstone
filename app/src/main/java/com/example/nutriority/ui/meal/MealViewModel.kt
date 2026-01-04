package com.example.nutriority.ui.meal

import android.content.SharedPreferences
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository,
    private val mealPlanner: MealPlanner,
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : ViewModel() {

    val allMeals: LiveData<List<Meal>> = mealRepository.allMeals.asLiveData()

    private val _mealPlan = MutableLiveData<List<List<Meal>>>()
    val mealPlan: LiveData<List<List<Meal>>> = _mealPlan

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadMealPlan()
    }

    fun generateMealPlan() {
        viewModelScope.launch {
            _isLoading.postValue(true)
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
                saveMealPlan(weeklyPlan)

            } else {
                Log.w("MealViewModel", "User not found, generating default meal plan.")
                val weeklyPlan = (1..7).map { 
                    mealPlanner.planMeals(2000, "Balanced", emptyList())
                }
                _mealPlan.postValue(weeklyPlan)
                saveMealPlan(weeklyPlan)
            }
            _isLoading.postValue(false)
        }
    }

    private fun saveMealPlan(weeklyPlan: List<List<Meal>>) {
        val json = gson.toJson(weeklyPlan)
        sharedPreferences.edit().putString(MEAL_PLAN_KEY, json).apply()
    }

    fun loadMealPlan() {
        val json = sharedPreferences.getString(MEAL_PLAN_KEY, null)
        if (json != null) {
            val type = object : TypeToken<List<List<Meal>>>() {}.type
            val weeklyPlan: List<List<Meal>> = gson.fromJson(json, type)
            _mealPlan.postValue(weeklyPlan)
        }
    }

    companion object {
        private const val MEAL_PLAN_KEY = "meal_plan"
    }
}
