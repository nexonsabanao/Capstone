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
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val mealRepository: MealRepository,
    private val sharedPreferences: SharedPreferences 
) : ViewModel() {

    val getUser: LiveData<User> = userRepository.getUser
    val sessionLogs: LiveData<List<WorkoutSessionLog>> = workoutRepository.getAllSessionLogs().asLiveData()
    val todayMealLogs: LiveData<List<DailyMealLog>> = mealRepository.getLogsForToday().asLiveData()

    fun updateWeight(weightKg: Double) {
        viewModelScope.launch {
            val currentUser = userRepository.getInitialUser() ?: return@launch
            userRepository.insertUser(currentUser.copy(weightKg = weightKg))
        }
    }

    fun logWeight(weightKg: Double, dateMillis: Long) {
        viewModelScope.launch {
            // Only update current user profile weight if the logged date is today or newer
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
