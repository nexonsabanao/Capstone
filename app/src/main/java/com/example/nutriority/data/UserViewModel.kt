package com.example.nutriority.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.User
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.planner.PlannerService
import com.example.nutriority.planner.WorkoutPlan
import com.example.nutriority.planner.WorkoutPlanner
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    private val plannerService: PlannerService,
    private val gson: Gson
) : ViewModel() {

    val userFlow: Flow<User?> = repository.getUser
    val user: LiveData<User?> = userFlow.asLiveData(viewModelScope.coroutineContext)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // We only sync if the plan is found in the user object but exercises might be missing.
        // This is safer than syncing on every single startup.
        viewModelScope.launch {
            val currentUser = repository.getInitialUser()
            if (currentUser != null && !currentUser.personalizedPlanJson.isNullOrBlank()) {
                // Background check/sync - won't block the UI
                workoutPlanner.syncPlanToDatabase(gson.fromJson(currentUser.personalizedPlanJson, WorkoutPlan::class.java))
            }
        }
    }

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
            syncEntirePlanToDb(updatedUser.personalizedPlanJson)
        }
    }

    fun restartWorkoutPlan() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = repository.getInitialUser() ?: return@launch
                
                val workoutPlan = withContext(Dispatchers.Default) {
                    workoutPlanner.planWorkouts(currentUser)
                }
                val workoutJson = gson.toJson(workoutPlan)
                
                val updatedUser = currentUser.copy(
                    personalizedPlanJson = workoutJson,
                    lastCompletedWorkoutDay = 0
                )
                
                repository.insertUser(updatedUser)
                // sync is already done in planWorkouts
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
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
                syncEntirePlanToDb(workoutPlanJson)
            }
            success
        }
    }

    suspend fun syncEntirePlanToDb(planJson: String?) {
        if (planJson.isNullOrBlank()) return
        try {
            val plan = gson.fromJson(planJson, WorkoutPlan::class.java)
            workoutPlanner.syncPlanToDatabase(plan)
        } catch (e: Exception) { }
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
}
