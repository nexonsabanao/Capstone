package com.example.nutriority.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.User
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.planner.WorkoutPlanner
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository,
    private val workoutPlanner: WorkoutPlanner,
    private val gson: Gson
) : ViewModel() {

    // Always observe the database directly for the strongest "Source of Truth"
    val user: LiveData<User> = repository.getUser

    /**
     * Updates specific onboarding data points and saves them to the DB.
     */
    fun updateOnboardingData(updateAction: (User) -> User) {
        viewModelScope.launch {
            val currentUser = repository.getInitialUser() ?: User(id = 1)
            val updatedUser = updateAction(currentUser)
            repository.insertUser(updatedUser)
        }
    }

    /**
     * Helper for screens that perform multiple updates before triggering a save.
     */
    fun saveOnboardingData() {
        // No-op in this new architecture as updateOnboardingData now saves instantly.
        // Keeping it to resolve unresolved references in existing screen logic.
    }

    suspend fun savePersonalizedPlanAndAwait(planJson: String): Boolean {
        return withContext(Dispatchers.IO) {
            val currentUser = repository.getInitialUser() ?: User(id = 1)
            val updatedUser = currentUser.copy(personalizedPlanJson = planJson)
            repository.insertUser(updatedUser)
        }
    }

    suspend fun completeWorkoutDay(dayIndex: Int) {
        withContext(Dispatchers.IO) {
            val currentUser = repository.getInitialUser() ?: return@withContext
            // Only increment if we are completing the current active day
            if (dayIndex == currentUser.lastCompletedWorkoutDay) {
                val updatedUser = currentUser.copy(lastCompletedWorkoutDay = dayIndex + 1)
                repository.insertUser(updatedUser)
            }
        }
    }

    suspend fun restartWorkoutPlan() {
        withContext(Dispatchers.IO) {
            val currentUser = repository.getInitialUser() ?: return@withContext
            val newPlan = workoutPlanner.planWorkouts(currentUser)
            val newPlanJson = gson.toJson(newPlan)

            val updatedUser = currentUser.copy(
                personalizedPlanJson = newPlanJson,
                lastCompletedWorkoutDay = 0
            )
            repository.insertUser(updatedUser)
        }
    }
}
