package com.example.nutriority.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {
    
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _selectedMealJson = MutableStateFlow<String?>(null)
    val selectedMealJson: StateFlow<String?> = _selectedMealJson.asStateFlow()

    private val _selectedArticleJson = MutableStateFlow<String?>(null)
    val selectedArticleJson: StateFlow<String?> = _selectedArticleJson.asStateFlow()

    private val _selectedWorkoutId = MutableStateFlow(-1)
    val selectedWorkoutId: StateFlow<Int> = _selectedWorkoutId.asStateFlow()

    private val _isPersonalizedFlow = MutableStateFlow(false)
    val isPersonalizedFlow: StateFlow<Boolean> = _isPersonalizedFlow.asStateFlow()

    private val _selectedDayIndex = MutableStateFlow(-1)
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()

    private val _selectedExerciseId = MutableStateFlow("")
    val selectedExerciseId: StateFlow<String> = _selectedExerciseId.asStateFlow()

    private val _exercisePosition = MutableStateFlow(-1)
    val exercisePosition: StateFlow<Int> = _exercisePosition.asStateFlow()

    private val _totalExercises = MutableStateFlow(-1)
    val totalExercises: StateFlow<Int> = _totalExercises.asStateFlow()

    // Using ArrayDeque for a more modern and efficient stack implementation
    private val backStack = ArrayDeque<Int>()

    fun setTab(index: Int, addToBackStack: Boolean = true) {
        if (index == _currentTab.value) return
        if (addToBackStack) {
            backStack.push(_currentTab.value)
        }
        _currentTab.value = index
    }

    fun navigateToMealDetail(json: String) {
        _selectedMealJson.value = json
        setTab(7) 
    }

    fun navigateToArticleDetail(json: String) {
        _selectedArticleJson.value = json
        setTab(8)
    }

    fun navigateToWorkoutDetail(workoutId: Int, isFromPersonalized: Boolean = false, dayIndex: Int = -1) {
        _selectedWorkoutId.value = workoutId
        _isPersonalizedFlow.value = isFromPersonalized
        _selectedDayIndex.value = dayIndex
        setTab(9)
    }

    fun navigateToExerciseDetail(workoutId: Int, exerciseId: String, position: Int, total: Int) {
        _selectedWorkoutId.value = workoutId
        _selectedExerciseId.value = exerciseId
        _exercisePosition.value = position
        _totalExercises.value = total
        setTab(10)
    }

    fun navigateToEditProfile() {
        setTab(12) 
    }

    fun navigateToWorkoutComplete() {
        setTab(11)
    }

    fun navigateToLogManual() {
        setTab(13)
    }

    fun goBack(): Boolean {
        if (backStack.isNotEmpty()) {
            _currentTab.value = backStack.pop()
            return true
        }
        return false
    }

    fun resetToHome() {
        backStack.clear()
        _currentTab.value = 0
    }
}
