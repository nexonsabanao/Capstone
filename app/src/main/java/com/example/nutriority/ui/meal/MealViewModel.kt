package com.example.nutriority.ui.meal

import android.app.Application
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.*
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

data class MealUiState(
    val items: List<MealListItem> = emptyList(),
    val isGenerating: Boolean = false,
    val isPlanExpired: Boolean = false,
    val hasPlan: Boolean = false,
    val targetCalories: Int = 0,
    val targetProtein: Int = 0,
    val targetCarbs: Int = 0,
    val targetFat: Int = 0,
    val isInitialLoading: Boolean = true // Flag for first database fetch
)

@HiltViewModel
class MealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    val userRepository: UserRepository,
    private val plannerService: PlannerService,
    private val application: Application
) : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)
    private val _swapState = MutableStateFlow<MealSwapState?>(null)
    val swapState: StateFlow<MealSwapState?> = _swapState.asStateFlow()

    val uiState: StateFlow<MealUiState> = combine(
        userRepository.getUser, 
        _isGenerating
    ) { user, isGenerating ->
        if (user == null) {
            // No user data yet - still loading from database
            MealUiState(isInitialLoading = true)
        } else if (user.mealPlanJson == null) {
            // User exists but has no meal plan
            MealUiState(isGenerating = isGenerating, isInitialLoading = false)
        } else {
            // User exists and has a plan
            val items = parseMealPlan(user.mealPlanJson)
            val isExpired = checkPlanExpired()
            
            val dailyCalories = plannerService.calculateDailyTarget(user)
            val macros = plannerService.calculateMacroTargets(dailyCalories)
            
            MealUiState(
                items = items,
                isGenerating = isGenerating,
                isPlanExpired = isExpired,
                hasPlan = items.isNotEmpty(),
                targetCalories = dailyCalories,
                targetProtein = macros.proteinGrams,
                targetCarbs = macros.carbsGrams,
                targetFat = macros.fatGrams,
                isInitialLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MealUiState(isInitialLoading = true)
    )

    fun generateNewMealPlan() {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val user = userRepository.getInitialUser() ?: return@launch
                val dailyCalories = plannerService.calculateDailyTarget(user)
                val mealPool = mealRepository.getAllMealsList()
                val weekPlan = withContext(Dispatchers.Default) {
                    (0 until 7).map {
                        async { 
                            plannerService.mealPlanner.planMeals(dailyCalories, user.preferredDiet, user.excludedIngredients, mealPool) 
                        }
                    }.awaitAll()
                }
                val json = Gson().toJson(weekPlan)
                
                userRepository.insertUser(user.copy(mealPlanJson = json))
                savePlanStartDate(LocalDate.now().toString())
            } catch (e: Exception) {
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun parseMealPlan(json: String): List<MealListItem> {
        return try {
            val gson = Gson()
            val plan: List<List<Meal>> = gson.fromJson(json, object : com.google.gson.reflect.TypeToken<List<List<Meal>>>() {}.type)
            val items = mutableListOf<MealListItem>()
            val startDateStr = getPlanStartDate()
            val startDate = if (startDateStr != null) LocalDate.parse(startDateStr) else LocalDate.now()

            plan.forEachIndexed { dayIndex, dayMeals ->
                val targetDate = startDate.plusDays(dayIndex.toLong())
                val dateHeader = if (dayIndex == 0) "Today" else if (dayIndex == 1) "Tomorrow" else targetDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                items.add(MealListItem.HeaderItem("$dateHeader, ${targetDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${targetDate.dayOfMonth}", dayIndex))

                dayMeals.sortedBy {
                    when(it.mealTime.lowercase()) {
                        "breakfast" -> 1
                        "lunch" -> 2
                        "dinner" -> 3
                        else -> 4
                    }
                }.forEach { meal ->
                    items.add(MealListItem.MealItem(meal, dayIndex))
                }
            }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun checkPlanExpired(): Boolean {
        val startDateStr = getPlanStartDate() ?: return false
        val startDate = LocalDate.parse(startDateStr)
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(startDate, today) >= 7
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
