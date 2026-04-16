package com.example.nutriority.ui.profile

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.WorkoutSessionLog
import com.example.nutriority.data.model.DailyMealLog
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.data.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val todayMealLogs: List<DailyMealLog> = emptyList(),
    val sessionLogs: List<WorkoutSessionLog> = emptyList(),
    val isLoading: Boolean = false,
    val isMealsLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val mealRepository: MealRepository,
    private val sharedPreferences: SharedPreferences 
) : ViewModel() {

    // Isolate the flows to ensure they start emitting immediately and don't block each other
    private val userFlow = userRepository.getUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Using null as initial value to represent the "loading" state from database
    private val mealsFlow = mealRepository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val sessionsFlow = workoutRepository.getAllSessionLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<ProfileUiState> = combine(
        userFlow,
        mealsFlow,
        sessionsFlow
    ) { user, allMeals, sessions ->
        // Filter for today's meals here to ensure it's always recalculated on any data change
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val start = today.timeInMillis
        
        today.add(Calendar.DAY_OF_MONTH, 1)
        val end = today.timeInMillis

        val todayMeals = allMeals?.filter { it.date in start until end } ?: emptyList()

        ProfileUiState(
            user = user,
            todayMealLogs = todayMeals,
            sessionLogs = sessions,
            isLoading = user == null,
            isMealsLoading = allMeals == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProfileUiState(isLoading = true, isMealsLoading = true)
    )

    val getUser: LiveData<User?> = userRepository.getUser.asLiveData()
    val sessionLogs: LiveData<List<WorkoutSessionLog>> = workoutRepository.getAllSessionLogs().asLiveData()

    fun updateWeight(weightKg: Double) {
        viewModelScope.launch {
            val currentUser = userRepository.getInitialUser() ?: return@launch
            userRepository.insertUser(currentUser.copy(weightKg = weightKg))
        }
    }

    fun logWeight(weightKg: Double, dateMillis: Long) {
        viewModelScope.launch {
            val targetCal = Calendar.getInstance().apply { 
                timeInMillis = dateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val targetStart = targetCal.timeInMillis
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (targetStart >= todayStart) {
                updateWeight(weightKg)
            }
            
            val allLogs = workoutRepository.getAllSessionLogs().first()
            val dayLogs = allLogs.filter { 
                val logCal = Calendar.getInstance().apply { timeInMillis = it.date }
                logCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                logCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
            }

            if (dayLogs.isNotEmpty()) {
                dayLogs.forEach { log ->
                    workoutRepository.insertSessionLog(log.copy(weightKg = weightKg))
                }
            } else {
                workoutRepository.insertSessionLog(
                    WorkoutSessionLog(
                        workoutId = 0,
                        workoutName = "Weight Log",
                        caloriesBurned = 0,
                        durationSeconds = 0,
                        date = dateMillis,
                        exercisesDone = 0,
                        totalExercises = 0,
                        difficulty = "N/A",
                        weightKg = weightKg
                    )
                )
            }
        }
    }

    fun logMeal(meal: Meal) {
        viewModelScope.launch {
            mealRepository.logMeal(meal)
            ensureActiveSessionLogged()
        }
    }

    private suspend fun ensureActiveSessionLogged() {
        val logs = workoutRepository.getAllSessionLogs().firstOrNull() ?: emptyList()
        val hasTodayLog = logs.any { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            val today = Calendar.getInstance()
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        }

        if (!hasTodayLog) {
            workoutRepository.insertSessionLog(
                WorkoutSessionLog(
                    workoutId = -1,
                    workoutName = "Daily Activity",
                    caloriesBurned = 0,
                    durationSeconds = 0,
                    date = System.currentTimeMillis(),
                    exercisesDone = 0,
                    totalExercises = 0,
                    difficulty = "N/A",
                    weightKg = userRepository.getInitialUser()?.weightKg ?: 0.0
                )
            )
        }
    }

    fun deleteMealLog(log: DailyMealLog) {
        viewModelScope.launch {
            mealRepository.deleteMealLog(log)
        }
    }

    suspend fun clearAllLocalData() {
        userRepository.deleteAll()
        workoutRepository.deleteAllHistory() 
        mealRepository.deleteAll()
        sharedPreferences.edit().clear().apply()
    }
}
