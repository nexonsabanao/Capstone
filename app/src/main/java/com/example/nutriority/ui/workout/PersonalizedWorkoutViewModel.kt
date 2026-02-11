package com.example.nutriority.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.planner.WorkoutPlan
import com.example.nutriority.planner.WorkoutSession
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonalizedWorkoutUiState(
    val sessions: List<WorkoutSession> = emptyList(),
    val lastCompletedDay: Int = 0,
    val currentWeek: Int = 0,
    val isLoading: Boolean = false,
    val hasPlan: Boolean = false,
    val isInitialLoading: Boolean = true
)

@HiltViewModel
class PersonalizedWorkoutViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gson: Gson
) : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)

    val uiState: StateFlow<PersonalizedWorkoutUiState> = combine(
        userRepository.getUser,
        _isGenerating
    ) { user, isGenerating ->
        if (user == null) {
            PersonalizedWorkoutUiState(isInitialLoading = true)
        } else if (user.personalizedPlanJson.isNullOrBlank()) {
            PersonalizedWorkoutUiState(isLoading = isGenerating, isInitialLoading = false, hasPlan = false)
        } else {
            try {
                val fullPlan = gson.fromJson(user.personalizedPlanJson, WorkoutPlan::class.java)
                val safeLastCompleted = user.lastCompletedWorkoutDay ?: 0
                val currentWeek = (safeLastCompleted / 7).coerceAtMost(3)
                val startIndex = currentWeek * 7
                val endIndex = (startIndex + 7).coerceAtMost(fullPlan.sessions.size)
                val activeSessions = fullPlan.sessions.subList(startIndex, endIndex)

                PersonalizedWorkoutUiState(
                    sessions = activeSessions,
                    lastCompletedDay = safeLastCompleted,
                    currentWeek = currentWeek,
                    isLoading = isGenerating,
                    hasPlan = true,
                    isInitialLoading = false
                )
            } catch (e: Exception) {
                PersonalizedWorkoutUiState(hasPlan = false, isInitialLoading = false)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PersonalizedWorkoutUiState(isInitialLoading = true)
    )

    fun completeWorkoutDay(dayIndex: Int) {
        viewModelScope.launch {
            val currentUser = userRepository.getInitialUser() ?: return@launch
            if (dayIndex == currentUser.lastCompletedWorkoutDay) {
                userRepository.insertUser(currentUser.copy(lastCompletedWorkoutDay = dayIndex + 1))
            }
        }
    }
}
