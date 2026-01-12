package com.example.nutriority.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Stack
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab

    private val _selectedMealJson = MutableStateFlow<String?>(null)
    val selectedMealJson: StateFlow<String?> = _selectedMealJson

    private val _selectedArticleJson = MutableStateFlow<String?>(null)
    val selectedArticleJson: StateFlow<String?> = _selectedArticleJson

    private val _selectedWorkoutId = MutableStateFlow(-1)
    val selectedWorkoutId: StateFlow<Int> = _selectedWorkoutId

    private val _selectedExerciseId = MutableStateFlow(-1)
    val selectedExerciseId: StateFlow<Int> = _selectedExerciseId

    private val _exercisePosition = MutableStateFlow(-1)
    val exercisePosition: StateFlow<Int> = _exercisePosition

    private val _totalExercises = MutableStateFlow(-1)
    val totalExercises: StateFlow<Int> = _totalExercises

    private val backStack = Stack<Int>()

    fun setTab(index: Int, addToBackStack: Boolean = true) {
        if (addToBackStack && _currentTab.value != index) {
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

    fun navigateToWorkoutDetail(workoutId: Int) {
        _selectedWorkoutId.value = workoutId
        setTab(9) // New index for Workout Detail
    }

    fun navigateToExerciseDetail(exerciseId: Int, position: Int, total: Int) {
        _selectedExerciseId.value = exerciseId
        _exercisePosition.value = position
        _totalExercises.value = total
        setTab(10)
    }

    fun goBack(): Boolean {
        if (backStack.isNotEmpty()) {
            _currentTab.value = backStack.pop()
            return true
        }
        return false
    }
}
