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
    val isInitialLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val mealRepository: MealRepository,
    private val sharedPreferences: SharedPreferences 
) : ViewModel() {

    // Main UI State following MealFragment pattern
    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.getUser,
        mealRepository.getLogsForToday(),
        workoutRepository.getAllSessionLogs()
    ) { user, meals, sessions ->
        ProfileUiState(
            user = user,
            todayMealLogs = meals,
            sessionLogs = sessions,
            isInitialLoading = user == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProfileUiState(isInitialLoading = true)
    )

    // Legacy LiveData if needed for specific triggers
    val getUser: LiveData<User?> = userRepository.getUser.asLiveData()
    val sessionLogs: LiveData<List<WorkoutSessionLog>> = workoutRepository.getAllSessionLogs().asLiveData()
    val todayMealLogs: LiveData<List<DailyMealLog>> = mealRepository.getLogsForToday().asLiveData()
    val allMealLogs: LiveData<List<DailyMealLog>> = mealRepository.getAllLogs().asLiveData()

    fun updateWeight(weightKg: Double) {
        viewModelScope.launch {
            val currentUser = userRepository.getInitialUser() ?: return@launch
            userRepository.insertUser(currentUser.copy(weightKg = weightKg))
        }
    }

    fun logWeight(weightKg: Double, dateMillis: Long) {
        viewModelScope.launch {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (dateMillis >= today) {
                updateWeight(weightKg)
            }
            
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
            mealRepository.deleteMealLog(log.id)
        }
    }

    suspend fun clearAllLocalData() {
        userRepository.deleteAll()
        workoutRepository.deleteAllHistory() 
        mealRepository.deleteAll()
        sharedPreferences.edit().clear().apply()
    }
}
