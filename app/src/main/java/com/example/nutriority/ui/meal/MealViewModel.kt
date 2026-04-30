package com.example.nutriority.ui.meal

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.model.DailyMealLog
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.planner.PlannerService
import com.example.nutriority.planner.DailyMacroTarget
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
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
    val isInitialLoading: Boolean = true 
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
        mealRepository.getAllLogs(),
        _isGenerating
    ) { user, logs, isGenerating ->
        if (user == null) {
            MealUiState(isInitialLoading = true)
        } else {
            val planJson = user.mealPlanJson
            if (planJson == null) {
                MealUiState(isGenerating = isGenerating, isInitialLoading = false)
            } else {
                val startDateStr = user.mealPlanStartDate ?: getPlanStartDateFromPrefs() ?: LocalDate.now().toString()
                
                val items = parseMealPlan(planJson, startDateStr, logs)
                val isExpired = checkPlanExpired(startDateStr)
                
                val dailyCalories = plannerService.calculateDailyTarget(user)
                val macros = plannerService.calculateMacroTargets(dailyCalories, user)
                
                MealUiState(
                    items = items,
                    isGenerating = isGenerating,
                    isPlanExpired = isExpired,
                    hasPlan = items.any { it is MealListItem.MealItem },
                    targetCalories = dailyCalories,
                    targetProtein = macros.proteinGrams,
                    targetCarbs = macros.carbsGrams,
                    targetFat = macros.fatGrams,
                    isInitialLoading = false
                )
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MealUiState(isInitialLoading = true)
    )

    fun generateNewMealPlan() {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val user = userRepository.getInitialUser() ?: return@launch
                
                var mealPool = mealRepository.getAllMealsList()
                if (mealPool.isEmpty()) {
                    mealRepository.syncMealsFromCloud()
                    mealPool = mealRepository.getAllMealsList()
                }

                if (mealPool.isEmpty()) return@launch

                val dailyCalories = plannerService.calculateDailyTarget(user)
                val macros = plannerService.calculateMacroTargets(dailyCalories, user)
                
                val dailyTarget = DailyMacroTarget(
                    calories = dailyCalories,
                    protein = macros.proteinGrams,
                    carbs = macros.carbsGrams,
                    fat = macros.fatGrams
                )
                
                val weekPlan = withContext(Dispatchers.Default) {
                    plannerService.mealPlanner.planWeek(
                        dailyTarget, 
                        user.preferredDiet, 
                        user.excludedIngredients, 
                        mealPool.shuffled()
                    )
                }

                if (weekPlan.any { it.isNotEmpty() }) {
                    val json = Gson().toJson(weekPlan)
                    val todayStr = LocalDate.now().toString()
                    savePlanStartDateToPrefs(todayStr) 
                    userRepository.insertUser(user.copy(
                        mealPlanJson = json,
                        mealPlanStartDate = todayStr
                    ))
                }
            } catch (e: Exception) {
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun parseMealPlan(json: String, startDateStr: String, logs: List<DailyMealLog>): List<MealListItem> {
        return try {
            val gson = Gson()
            val plan: List<List<Meal>> = gson.fromJson(json, object : com.google.gson.reflect.TypeToken<List<List<Meal>>>() {}.type)
            val items = mutableListOf<MealListItem>()
            
            val startDate = LocalDate.parse(startDateStr)
            val today = LocalDate.now()

            plan.forEachIndexed { dayIndex, dayMeals ->
                val targetDate = startDate.plusDays(dayIndex.toLong())
                val daysDiff = ChronoUnit.DAYS.between(today, targetDate).toInt()
                
                val formattedDate = targetDate.format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()))
                val dateHeader = when (daysDiff) {
                    -1 -> "Yesterday, $formattedDate"
                    0 -> "Today, $formattedDate"
                    1 -> "Tomorrow, $formattedDate"
                    else -> targetDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
                }
                
                items.add(MealListItem.HeaderItem(dateHeader, dayIndex))

                dayMeals.sortedBy {
                    when(it.mealTime.lowercase()) {
                        "breakfast" -> 1
                        "lunch" -> 2
                        "dinner" -> 3
                        else -> 4
                    }
                }.forEach { meal ->
                    val isLogged = logs.any { log ->
                        val logDate = Instant.ofEpochMilli(log.date).atZone(ZoneId.systemDefault()).toLocalDate()
                        log.mealId == meal.id && logDate == targetDate
                    }
                    items.add(MealListItem.MealItem(meal, dayIndex, isLogged))
                }
            }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun checkPlanExpired(startDateStr: String?): Boolean {
        val startDate = if (startDateStr != null) LocalDate.parse(startDateStr) else return false
        return ChronoUnit.DAYS.between(startDate, LocalDate.now()) >= 7
    }

    fun deleteMealPlan() {
        viewModelScope.launch {
            val user = userRepository.getInitialUser()
            if (user != null) {
                userRepository.insertUser(user.copy(mealPlanJson = null, mealPlanStartDate = null))
            }
        }
    }

    private fun savePlanStartDateToPrefs(date: String) {
        val prefs = application.getSharedPreferences("meal_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("plan_start_date", date).apply()
    }

    private fun getPlanStartDateFromPrefs(): String? {
        val prefs = application.getSharedPreferences("meal_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("plan_start_date", null)
    }

    fun swapMeal(mealToReplace: Meal, dayIndex: Int) {
        viewModelScope.launch {
            val user = userRepository.getInitialUser() ?: return@launch
            val allMeals = mealRepository.getAllMealsList()
            
            val options = allMeals.filter { meal ->
                meal.mealTime.equals(mealToReplace.mealTime, ignoreCase = true) && 
                meal.id != mealToReplace.id &&
                isMealCompatible(meal, user.preferredDiet, user.excludedIngredients)
            }
            
            _swapState.value = MealSwapState(mealToReplace, dayIndex, options)
        }
    }

    private fun isMealCompatible(meal: Meal, preferredDiet: String, excludedIngredients: List<String>): Boolean {
        val isExcluded = excludedIngredients.any { excluded ->
            val normalizedExcluded = normalizeIngredient(excluded)
            meal.ingredients.any { ingredient ->
                val normalizedIngredient = normalizeIngredient(ingredient)
                normalizedIngredient.contains(normalizedExcluded, true) || 
                normalizedExcluded.contains(normalizedIngredient, true)
            }
        }
        if (isExcluded) return false

        if (preferredDiet.isNotBlank() && !preferredDiet.equals("Balanced", ignoreCase = true)) {
            if (meal.preferredDiet.isNotBlank() && 
                !meal.preferredDiet.equals("Balanced", ignoreCase = true) && 
                !meal.preferredDiet.equals(preferredDiet, ignoreCase = true)) {
                return false
            }
        }

        return when (preferredDiet.lowercase()) {
            "vegetarian" -> !containsMeat(meal)
            "low carb", "low-carb" -> isLowCarb(meal)
            else -> true 
        }
    }

    private fun normalizeIngredient(input: String): String {
        val lower = input.lowercase().trim()
        return if (lower.endsWith("s") && lower.length > 3) {
            lower.substring(0, lower.length - 1)
        } else {
            lower
        }
    }

    private fun containsMeat(meal: Meal): Boolean = meal.ingredients.any { 
        val norm = normalizeIngredient(it)
        norm.contains("chicken") || norm.contains("beef") || norm.contains("pork") 
    }
    
    private fun isLowCarb(meal: Meal): Boolean = !meal.ingredients.any { 
        val norm = normalizeIngredient(it)
        norm.contains("bread") || norm.contains("pasta") || norm.contains("rice") || norm.contains("potato") 
    }

    fun onSwapMealSelected(oldMeal: Meal, newMeal: Meal, dayIndex: Int) {
        viewModelScope.launch {
            val user = userRepository.getInitialUser() ?: return@launch
            val planJson = user.mealPlanJson ?: return@launch
            val gson = Gson()
            val plan: MutableList<MutableList<Meal>> = gson.fromJson(planJson, object : com.google.gson.reflect.TypeToken<MutableList<MutableList<Meal>>>() {}.type)

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
}
