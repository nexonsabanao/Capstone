package com.example.nutriority.ui.meal

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.planner.PlannerService
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class MealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository,
    private val plannerService: PlannerService,
    private val application: Application
) : ViewModel() {

    private val _isGenerating = MutableLiveData<Boolean>()
    val isGenerating: LiveData<Boolean> = _isGenerating

    private val _isPlanExpired = MutableLiveData<Boolean>()
    val isPlanExpired: LiveData<Boolean> = _isPlanExpired

    val currentMealPlan: StateFlow<List<MealListItem>>

    init {
        currentMealPlan = combine(userRepository.getUser.asFlow(), mealRepository.allMeals) { user, allMeals ->
            if (user?.mealPlanJson == null) {
                return@combine emptyList()
            }

            try {
                val gson = Gson()
                val plan: List<List<Meal>> = gson.fromJson(user.mealPlanJson, object : com.google.gson.reflect.TypeToken<List<List<Meal>>>() {}.type)
                val items = mutableListOf<MealListItem>()
                
                val startDateStr = getPlanStartDate()
                val startDate = if (startDateStr != null) LocalDate.parse(startDateStr) else LocalDate.now()
                val today = LocalDate.now()
                val daysPassed = ChronoUnit.DAYS.between(startDate, today)
                
                _isPlanExpired.postValue(daysPassed >= 7)

                plan.forEachIndexed { dayIndex, dayMeals ->
                    val targetDate = startDate.plusDays(dayIndex.toLong())
                    val dateHeader = if (dayIndex == 0) "Today" else if (dayIndex == 1) "Tomorrow" else targetDate.dayOfWeek.name.lowercase().capitalize()
                    
                    items.add(MealListItem.HeaderItem("$dateHeader, ${targetDate.month.name.lowercase().capitalize()} ${targetDate.dayOfMonth}", dayIndex))
                    
                    // Standardize order: Breakfast, Lunch, Dinner
                    val sortedMeals = dayMeals.sortedBy { 
                        when(it.mealTime.lowercase()) {
                            "breakfast" -> 1
                            "lunch" -> 2
                            "dinner" -> 3
                            else -> 4
                        }
                    }
                    
                    sortedMeals.forEach { meal ->
                        items.add(MealListItem.MealItem(meal, dayIndex))
                    }
                }
                items
            } catch (e: Exception) {
                emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun generateNewMealPlan() {
        viewModelScope.launch {
            _isGenerating.value = true
            val user = userRepository.getInitialUser()
            if (user != null) {
                val dailyCalories = com.example.nutriority.planner.NutritionCalculator.calculateTdeeDailyCalories(
                    user.weightKg, user.heightCm, user.age ?: 30, user.gender, user.activityLevel, user.goal
                ).toInt()

                val weekPlan = mutableListOf<List<Meal>>()
                for (i in 0 until 7) {
                    val dayMeals = plannerService.generatePlanForUser(user).nutritionPlan.meals
                    weekPlan.add(dayMeals)
                }

                val json = Gson().toJson(weekPlan)
                userRepository.insertUser(user.copy(mealPlanJson = json))
                savePlanStartDate(LocalDate.now().toString())
            }
            _isGenerating.value = false
        }
    }

    fun swapMeal(oldMeal: Meal, dayIndex: Int) {
        viewModelScope.launch {
            val user = userRepository.getInitialUser() ?: return@launch
            val gson = Gson()
            val plan: MutableList<MutableList<Meal>> = gson.fromJson(user.mealPlanJson, object : com.google.gson.reflect.TypeToken<MutableList<MutableList<Meal>>>() {}.type)
            
            val allMeals = mealRepository.getAllMealsList()
            val newMeal = allMeals.filter { it.mealTime == oldMeal.mealTime && it.id != oldMeal.id }.randomOrNull()
            
            if (newMeal != null) {
                val dayMeals = plan[dayIndex]
                val index = dayMeals.indexOfFirst { it.id == oldMeal.id }
                if (index != -1) {
                    dayMeals[index] = newMeal
                    val updatedJson = gson.toJson(plan)
                    userRepository.insertUser(user.copy(mealPlanJson = updatedJson))
                }
            }
        }
    }

    private fun savePlanStartDate(date: String) {
        val prefs = application.getSharedPreferences("meal_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("plan_start_date", date).apply()
    }

    private fun getPlanStartDate(): String? {
        val prefs = application.getSharedPreferences("meal_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("plan_start_date", null)
    }
}
