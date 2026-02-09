package com.example.nutriority.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.User
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.planner.WorkoutPlan
import com.example.nutriority.planner.WorkoutPlanner
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository,
    private val workoutPlanner: WorkoutPlanner,
    private val gson: Gson
) : ViewModel() {

    val user: LiveData<User> = repository.getUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var lastSyncedWeekIndex = -1

    fun updateOnboardingData(updateAction: (User) -> User) {
        viewModelScope.launch {
            updateOnboardingDataSuspend(updateAction)
        }
    }

    suspend fun updateOnboardingDataSuspend(updateAction: (User) -> User) {
        withContext(Dispatchers.IO) {
            val currentUser = repository.getInitialUser() ?: User(id = 1)
            val updatedUser = updateAction(currentUser)
            repository.insertUser(updatedUser)
            
            if (!updatedUser.personalizedPlanJson.isNullOrBlank()) {
                try {
                    val plan = gson.fromJson(updatedUser.personalizedPlanJson, WorkoutPlan::class.java)
                    workoutPlanner.syncPlanToDatabase(plan)
                } catch (e: Exception) { }
            }
        }
    }

    suspend fun saveFullPlan(workoutPlanJson: String, mealPlanJson: String): Boolean {
        return withContext(Dispatchers.IO) {
            val currentUser = repository.getInitialUser() ?: User(id = 1)
            val updatedUser = currentUser.copy(
                personalizedPlanJson = workoutPlanJson,
                mealPlanJson = mealPlanJson
            )
            val success = repository.insertUser(updatedUser)
            
            if (success) {
                try {
                    val plan = gson.fromJson(workoutPlanJson, WorkoutPlan::class.java)
                    workoutPlanner.syncPlanToDatabase(plan)
                    lastSyncedWeekIndex = 0
                } catch (e: Exception) { }
            }
            success
        }
    }

    suspend fun ensurePlanSynced(plan: WorkoutPlan, dayIndex: Int): Boolean {
        val currentWeek = dayIndex / 7
        if (currentWeek == lastSyncedWeekIndex) return false
        
        return withContext(Dispatchers.IO) {
            val startIndex = currentWeek * 7
            val endIndex = (startIndex + 7).coerceAtMost(plan.sessions.size)
            val activeSessions = plan.sessions.subList(startIndex, endIndex)
            
            workoutPlanner.syncPlanToDatabase(plan.copy(sessions = activeSessions))
            lastSyncedWeekIndex = currentWeek
            true
        }
    }

    suspend fun savePersonalizedPlanAndAwait(planJson: String): Boolean {
        return withContext(Dispatchers.IO) {
            val currentUser = repository.getInitialUser() ?: User(id = 1)
            val updatedUser = currentUser.copy(personalizedPlanJson = planJson)
            val success = repository.insertUser(updatedUser)
            
            if (success) {
                try {
                    val plan = gson.fromJson(planJson, WorkoutPlan::class.java)
                    workoutPlanner.syncPlanToDatabase(plan)
                    lastSyncedWeekIndex = 0
                } catch (e: Exception) { }
            }
            success
        }
    }

    suspend fun completeWorkoutDay(dayIndex: Int) {
        withContext(Dispatchers.IO) {
            val currentUser = repository.getInitialUser() ?: return@withContext
            if (dayIndex == currentUser.lastCompletedWorkoutDay) {
                val updatedUser = currentUser.copy(lastCompletedWorkoutDay = dayIndex + 1)
                repository.insertUser(updatedUser)
            }
        }
    }

    suspend fun restartWorkoutPlan() {
        _isLoading.value = true
        withContext(Dispatchers.IO) {
            val currentUser = repository.getInitialUser() ?: return@withContext
            val newPlan = workoutPlanner.planWorkouts(currentUser)
            val newPlanJson = gson.toJson(newPlan)

            val updatedUser = currentUser.copy(
                personalizedPlanJson = newPlanJson,
                lastCompletedWorkoutDay = 0
            )
            repository.insertUser(updatedUser)
            lastSyncedWeekIndex = -1 // Force re-sync
        }
        _isLoading.value = false
    }
}