package com.example.nutriority.ui.meal

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.planner.PlannerService
import com.example.nutriority.ui.util.AgeUtil
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class MealSwapState(
    val mealToReplace: Meal,
    val dayIndex: Int,
    val options: List<Meal>
)

@HiltViewModel
class MealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    val userRepository: UserRepository,
    private val plannerService: PlannerService,
    private val application: Application
) : ViewModel() {

    private val _isGenerating = MutableLiveData<Boolean>(false)
    val isGenerating: LiveData<Boolean> = _isGenerating

    private val _isPlanExpired = MutableLiveData<Boolean>()
    val isPlanExpired: LiveData<Boolean> = _isPlanExpired

    private val _swapState = MutableStateFlow<MealSwapState?>(null)
    val swapState: StateFlow<MealSwapState?> = _swapState.asStateFlow()

    val currentMealPlan: StateFlow<List<MealListItem>>

    init {
        currentMealPlan = userRepository.getUser.asFlow()
            .map { it?.mealPlanJson }
            .distinctUntilChanged()
            .map { mealPlanJson ->
                if (mealPlanJson == null) {
                    return@map emptyList<MealListItem>()
                }

                try {
                    val gson = Gson()
                    val plan: List<List<Meal>> = gson.fromJson(mealPlanJson, object : com.google.gson.reflect.TypeToken<List<List<Meal>>>() {}.type)
                    val items = mutableListOf<MealListItem>()

                    val startDateStr = getPlanStartDate()
                    val startDate = if (startDateStr != null) LocalDate.parse(startDateStr) else LocalDate.now()
                    val today = LocalDate.now()
                    val daysPassed = ChronoUnit.DAYS.between(startDate, today)

                    _isPlanExpired.postValue(daysPassed >= 7)

                    plan.forEachIndexed { dayIndex, dayMeals ->
                        val targetDate = startDate.plusDays(dayIndex.toLong())
                        val dateHeader = if (dayIndex == 0) "Today" else if (dayIndex == 1) "Tomorrow" else targetDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

                        items.add(MealListItem.HeaderItem("$dateHeader, ${targetDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${targetDate.dayOfMonth}", dayIndex))

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
                    emptyList<MealListItem>()
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )
    }

    fun generateNewMealPlan() {
        viewModelScope.launch {
            _isGenerating.value = true
            
            val result = withContext(Dispatchers.Default) {
                try {
                    val user = userRepository.getInitialUser()
                    if (user != null) {
                        val dailyCalories = com.example.nutriority.planner.NutritionCalculator.calculateTdeeDailyCalories(
                            user.weightKg, user.heightCm, AgeUtil.calculateAge(user.birthDate), user.gender, user.activityLevel, user.goal
                        )

                        // Prefetch meals outside the loop
                        val mealPool = mealRepository.getAllMealsList()

                        val weekPlan = (0 until 7).map {
                            async { 
                                plannerService.mealPlanner.planMeals(
                                    dailyCalories, 
                                    user.preferredDiet, 
                                    user.excludedIngredients,
                                    mealPool
                                )
                            }
                        }.awaitAll()

                        val json = Gson().toJson(weekPlan)
                        Triple(true, user, json)
                    } else {
                        Triple(false, null, null)
                    }
                } catch (e: Exception) {
                    Triple(false, null, null)
                }
            }

            if (result.first) {
                val user = result.second!!
                val json = result.third!!
                userRepository.insertUser(user.copy(mealPlanJson = json))
                savePlanStartDate(LocalDate.now().toString())
            }
            
            _isGenerating.value = false
        }
    }

    fun deleteMealPlan() {
        viewModelScope.launch {
            val user = userRepository.getInitialUser()
            if (user != null) {
                userRepository.insertUser(user.copy(mealPlanJson = null))
            }
        }
    }

    fun swapMeal(mealToReplace: Meal, dayIndex: Int) {
        viewModelScope.launch {
            val allMeals = mealRepository.getAllMealsList()
            val options = allMeals.filter { it.mealTime == mealToReplace.mealTime && it.id != mealToReplace.id }
            _swapState.value = MealSwapState(mealToReplace, dayIndex, options)
        }
    }

    fun onSwapMealSelected(oldMeal: Meal, newMeal: Meal, dayIndex: Int) {
        viewModelScope.launch {
            val user = userRepository.getInitialUser() ?: return@launch
            val gson = Gson()
            val plan: MutableList<MutableList<Meal>> = gson.fromJson(user.mealPlanJson, object : com.google.gson.reflect.TypeToken<MutableList<MutableList<Meal>>>() {}.type)

            val dayMeals = plan[dayIndex]
            val index = dayMeals.indexOfFirst { it.id == oldMeal.id }
            if (index != -1) {
                dayMeals[index] = newMeal
                val updatedJson = gson.toJson(plan)
                userRepository.insertUser(user.copy(mealPlanJson = updatedJson))
            }
        }
        _swapState.value = null
    }

    fun onSwapCancelled() {
        _swapState.value = null
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