package com.example.nutriority.ui.meal

import android.app.Application
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
import kotlinx.coroutines.flow.collectLatest
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
    private val gson: Gson,
    private val application: Application
) : ViewModel() {

    val allMeals: LiveData<List<Meal>> = mealRepository.allMeals.asLiveData()
    private var cachedMeals: List<Meal> = emptyList()

    private val _mealPlan = MutableLiveData<List<List<Meal>>>()
    val mealPlan: LiveData<List<List<Meal>>> = _mealPlan

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isPlanExpired = MutableLiveData<Boolean>()
    val isPlanExpired: LiveData<Boolean> = _isPlanExpired

    init {
        viewModelScope.launch {
            mealRepository.allMeals.collectLatest {
                cachedMeals = it
                Log.d("MealViewModel", "Library meals cached: ${it.size}")
            }
        }
        loadMealPlan()
    }

    fun generateMealPlan() {
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
                saveMealPlanToLocalAndCloud(weeklyPlan)
                _isPlanExpired.postValue(false)
            } catch (e: Exception) {
                Log.e("MealViewModel", "Error generating meal plan", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun swapMeal(dayIndex: Int, mealToReplace: Meal, newMeal: Meal) {
        val currentPlan = _mealPlan.value?.toMutableList() ?: return
        if (dayIndex >= currentPlan.size) return
        
        val dailyMeals = currentPlan[dayIndex].toMutableList()
        val indexInDay = dailyMeals.indexOfFirst { it.time == mealToReplace.time && it.name == mealToReplace.name }
        
        if (indexInDay != -1) {
            dailyMeals[indexInDay] = newMeal.copy(time = mealToReplace.time)
            currentPlan[dayIndex] = dailyMeals
            _mealPlan.postValue(currentPlan)
            saveMealPlanToLocalAndCloud(currentPlan)
        }
    }

    fun getSwapOptions(mealToReplace: Meal): List<Meal> {
        val mealType = mealToReplace.time
        
        // Use allMeals.value as primary source if available, otherwise cachedMeals
        val source = allMeals.value ?: cachedMeals
        
        val options = source.filter { 
            it.time.equals(mealType, ignoreCase = true) && 
            it.name != mealToReplace.name 
        }.shuffled().take(10)
        
        Log.d("MealViewModel", "Swap options for $mealType: ${options.size} found in ${source.size} total meals")
        return options
    }

    private fun saveMealPlanToLocalAndCloud(weeklyPlan: List<List<Meal>>) {
        try {
            val json = gson.toJson(weeklyPlan)
            
            // 1. Save to Local Prefs
            sharedPreferences.edit()
                .putString(MEAL_PLAN_KEY, json)
                .putLong(MEAL_PLAN_START_DATE_KEY, System.currentTimeMillis())
                .apply()

            // 2. Save to Cloud
            viewModelScope.launch {
                userRepository.getInitialUser()?.let { user ->
                    userRepository.insertUser(user.copy(mealPlanJson = json))
                }
            }
        } catch (e: Exception) {
            Log.e("MealViewModel", "Error saving meal plan", e)
        }
    }

    fun loadMealPlan() {
        viewModelScope.launch {
            val localJson = sharedPreferences.getString(MEAL_PLAN_KEY, null)
            val startMillis = sharedPreferences.getLong(MEAL_PLAN_START_DATE_KEY, -1)

            if (localJson != null && startMillis != -1L) {
                displayJsonPlan(localJson, startMillis)
            } else {
                val user = userRepository.getInitialUser()
                val cloudJson = user?.mealPlanJson
                if (!cloudJson.isNullOrBlank()) {
                    displayJsonPlan(cloudJson, System.currentTimeMillis())
                } else {
                    _mealPlan.postValue(emptyList())
                    _isPlanExpired.postValue(false)
                }
            }
        }
    }

    private fun displayJsonPlan(json: String, startMillis: Long) {
        try {
            val type = object : TypeToken<List<List<Meal>>>() {}.type
            val weeklyPlan: List<List<Meal>> = gson.fromJson(json, type)
            
            // Resolve imageResIds for the plan
            val resources = application.resources
            val packageName = application.packageName
            weeklyPlan.forEach { daily ->
                daily.forEach { meal ->
                    if (meal.imageResId == 0) {
                        meal.imageResId = resources.getIdentifier(meal.imageName, "drawable", packageName)
                    }
                }
            }
            
            _mealPlan.postValue(weeklyPlan)

            val startDate = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val today = LocalDate.now()
            val daysPassed = ChronoUnit.DAYS.between(startDate, today)
            _isPlanExpired.postValue(daysPassed >= 7)
        } catch (e: Exception) {
            _mealPlan.postValue(emptyList())
        }
    }

    fun completeMealPlan() {
        sharedPreferences.edit()
            .remove(MEAL_PLAN_KEY)
            .remove(MEAL_PLAN_START_DATE_KEY)
            .apply()
        
        viewModelScope.launch {
            userRepository.getInitialUser()?.let { user ->
                userRepository.insertUser(user.copy(mealPlanJson = null))
            }
        }
        
        _mealPlan.postValue(emptyList())
        _isPlanExpired.postValue(false)
    }

    fun getDayLabel(dayIndex: Int): String {
        val startMillis = sharedPreferences.getLong(MEAL_PLAN_START_DATE_KEY, System.currentTimeMillis())
        val startDate = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val targetDate = startDate.plusDays(dayIndex.toLong())
        return targetDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }

    companion object {
        private const val MEAL_PLAN_KEY = "meal_plan"
        private const val MEAL_PLAN_START_DATE_KEY = "meal_plan_start_date"
    }
}
