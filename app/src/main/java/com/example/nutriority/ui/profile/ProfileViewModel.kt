package com.example.nutriority.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.WorkoutSessionLog
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    val getUser: LiveData<User> = userRepository.getUser
    
    // Updated to observe Session Logs instead of individual exercise logs
    val sessionLogs: LiveData<List<WorkoutSessionLog>> = workoutRepository.getAllSessionLogs().asLiveData()

    fun updateWeight(weightKg: Double) {
        viewModelScope.launch {
            val currentUser = userRepository.getInitialUser() ?: return@launch
            userRepository.insertUser(currentUser.copy(weightKg = weightKg))
        }
    }
}
