package com.example.nutriority.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    private val workoutPlanner: WorkoutPlanner, // Injected the planner
    private val gson: Gson
) : ViewModel() {

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    init {
        viewModelScope.launch {
            val initialUser = repository.getInitialUser() ?: User(id = 1)
            _user.postValue(initialUser)
        }
    }

    fun updateOnboardingData(updateAction: (User) -> User) {
        val updatedUser = updateAction(_user.value ?: User(id = 1))
        _user.value = updatedUser
    }

    fun saveOnboardingData() {
        _user.value?.let { userToSave ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertUser(userToSave)
            }
        }
    }

    suspend fun savePersonalizedPlanAndAwait(planJson: String): Boolean {
        return withContext(Dispatchers.IO) {
            _user.value?.let { currentUser ->
                val updatedUser = currentUser.copy(personalizedPlanJson = planJson)
                _user.postValue(updatedUser) // postValue to update LiveData from background thread
                repository.insertUser(updatedUser)
            } ?: false
        }
    }

    suspend fun completeWorkoutDay(dayIndex: Int) {
        withContext(Dispatchers.IO) {
            _user.value?.let { currentUser ->
                val updatedUser = currentUser.copy(lastCompletedWorkoutDay = dayIndex + 1)
                _user.postValue(updatedUser)
                repository.insertUser(updatedUser)
            }
        }
    }

    suspend fun restartWorkoutPlan() {
        withContext(Dispatchers.IO) {
            _user.value?.let { currentUser ->
                // 1. Generate a new plan using the genius planner
                val newPlan = workoutPlanner.planWorkouts(currentUser)
                val newPlanJson = gson.toJson(newPlan)

                // 2. Save the new plan and reset the completion day
                val updatedUser = currentUser.copy(
                    personalizedPlanJson = newPlanJson,
                    lastCompletedWorkoutDay = 0
                )
                _user.postValue(updatedUser)
                repository.insertUser(updatedUser)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
