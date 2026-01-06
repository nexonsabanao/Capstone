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
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isPlanExpired = MutableLiveData<Boolean>()
    val isPlanExpired: LiveData<Boolean> = _isPlanExpired

    init {
        loadMealPlan()
    }

    fun generateMealPlan() {
        val startMillis = sharedPreferences.getLong(MEAL_PLAN_START_DATE_KEY, -1)
        if (startMillis != -1L) {
            val startDate = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val today = LocalDate.now()
            if (ChronoUnit.DAYS.between(startDate, today) < 7) {
                Log.d("MealViewModel", "Valid meal plan exists. Skipping generation.")
                if (_mealPlan.value.isNullOrEmpty()) {
                    loadMealPlan()
                }
                return
            }
        }

        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val user = userRepository.getInitialUser()
                val dailyCalories = if (user != null) {
                    NutritionCalculator.calculateTdeeDailyCalories(
                        user.weightKg,
                        user.heightCm,
                        user.age ?: 30,
                        user.gender,
                        user.activityLevel,
                        user.goal
                    )
                } else {
                    Log.w("MealViewModel", "User not found, using default calories.")
                    2000
                }

                val weeklyPlan = (0 until 7).map {
                    mealPlanner.planMeals(
                        dailyCalories,
                        user?.preferredDiet ?: "Balanced",
                        user?.excludedIngredients ?: emptyList()
                    )
                }

                _mealPlan.postValue(weeklyPlan)
                saveMealPlan(weeklyPlan)

                sharedPreferences.edit()
                    .putLong(MEAL_PLAN_START_DATE_KEY, System.currentTimeMillis())
                    .apply()
                _isPlanExpired.postValue(false)
            } catch (e: Exception) {
                Log.e("MealViewModel", "Error generating meal plan", e)
                // Potentially post an error state to the UI
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun saveMealPlan(weeklyPlan: List<List<Meal>>) {
        try {
            val json = gson.toJson(weeklyPlan)
            sharedPreferences.edit()
                .putString(MEAL_PLAN_KEY, json)
                .apply()
        } catch (e: Exception) {
            Log.e("MealViewModel", "Error saving meal plan", e)
        }
    }

    fun loadMealPlan() {
        val json = sharedPreferences.getString(MEAL_PLAN_KEY, null)
        val startMillis = sharedPreferences.getLong(MEAL_PLAN_START_DATE_KEY, -1)

        if (json != null && startMillis != -1L) {
            try {
                val type = object : TypeToken<List<List<Meal>>>() {}.type
                val weeklyPlan: List<List<Meal>> = gson.fromJson(json, type)
                _mealPlan.postValue(weeklyPlan)

                val startDate = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                val today = LocalDate.now()
                val daysPassed = ChronoUnit.DAYS.between(startDate, today)
                _isPlanExpired.postValue(daysPassed >= 7)
            } catch (e: JsonSyntaxException) {
                Log.e("MealViewModel", "Error parsing meal plan from JSON", e)
                completeMealPlan() // Clear corrupted data
            }
        } else {
            _mealPlan.postValue(emptyList())
            _isPlanExpired.postValue(false)
        }
    }

    fun completeMealPlan() {
        sharedPreferences.edit()
            .remove(MEAL_PLAN_KEY)
            .remove(MEAL_PLAN_START_DATE_KEY)
            .apply()
        _mealPlan.postValue(emptyList())
        _isPlanExpired.postValue(false)
    }

    fun getDayLabel(dayIndex: Int): String {
        val startMillis = sharedPreferences.getLong(MEAL_PLAN_START_DATE_KEY, -1)
        if (startMillis == -1L) return "Day ${dayIndex + 1}"

        val startDate = Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val targetDate = startDate.plusDays(dayIndex.toLong())
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(today, targetDate)
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

        return when (daysBetween) {
            0L -> "Today, ${targetDate.format(dateFormatter)}"
            -1L -> "Yesterday, ${targetDate.format(dateFormatter)}"
            1L -> "Tomorrow, ${targetDate.format(dateFormatter)}"
            else -> {
                if (daysBetween > 1) { // Future days
                    val futureFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
                    targetDate.format(futureFormatter)
                } else { // Past days (older than yesterday)
                    val daysAgo = -daysBetween
                    "$daysAgo days ago, ${targetDate.format(dateFormatter)}"
                }
            }
        }
    }

    companion object {
        private const val MEAL_PLAN_KEY = "meal_plan"
        private const val MEAL_PLAN_START_DATE_KEY = "meal_plan_start_date"
    }
}
