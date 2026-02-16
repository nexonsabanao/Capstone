package com.example.nutriority.ui

import androidx.lifecycle.ViewModel
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.planner.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * Atomic navigation request to prevent flickering and race conditions
 * when switching between different types of workouts.
 */
data class WorkoutNavRequest(
    val workoutId: Int = -1,
    val session: WorkoutSession? = null,
    val isFromPersonalized: Boolean = false,
    val dayIndex: Int = -1,
    val timestamp: Long = 0L 
)

@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {
    
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _selectedMealJson = MutableStateFlow<String?>(null)
    val selectedMealJson: StateFlow<String?> = _selectedMealJson.asStateFlow()

    private val _selectedArticleJson = MutableStateFlow<String?>(null)
    val selectedArticleJson: StateFlow<String?> = _selectedArticleJson.asStateFlow()

    // BUNDLED STATE: The only source of truth for workout navigation
    private val _workoutNavRequest = MutableStateFlow(WorkoutNavRequest())
    val workoutNavRequest: StateFlow<WorkoutNavRequest> = _workoutNavRequest.asStateFlow()

    // Exercise details
    private val _selectedExerciseId = MutableStateFlow("")
    val selectedExerciseId: StateFlow<String> = _selectedExerciseId.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _exercisePosition = MutableStateFlow(-1)
    val exercisePosition: StateFlow<Int> = _exercisePosition.asStateFlow()

    private val _totalExercises = MutableStateFlow(-1)
    val totalExercises: StateFlow<Int> = _totalExercises.asStateFlow()

    private val _currentWorkoutWithExercises = MutableStateFlow<WorkoutWithExercises?>(null)
    val currentWorkoutWithExercises: StateFlow<WorkoutWithExercises?> = _currentWorkoutWithExercises.asStateFlow()

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

    fun navigateToWorkoutDetail(workoutId: Int, session: WorkoutSession? = null, isFromPersonalized: Boolean = false, dayIndex: Int = -1) {
        _workoutNavRequest.value = WorkoutNavRequest(
            workoutId = workoutId,
            session = session,
            isFromPersonalized = isFromPersonalized,
            dayIndex = dayIndex,
            timestamp = System.currentTimeMillis()
        )
        _selectedExerciseId.value = ""
        _selectedCategory.value = ""
        _exercisePosition.value = -1
        setTab(9)
    }

    fun navigateToExerciseDetail(workoutId: Int, exerciseId: String, category: String, position: Int, total: Int) {
        _workoutNavRequest.value = _workoutNavRequest.value.copy(workoutId = workoutId)
        _selectedExerciseId.value = exerciseId
        _selectedCategory.value = category
        _exercisePosition.value = position
        _totalExercises.value = total
        setTab(10)
    }

    fun setCurrentWorkoutData(workout: WorkoutWithExercises) {
        _currentWorkoutWithExercises.value = workout
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

    fun navigateToAllMeals() {
        setTab(14)
    }

    fun nextExercise() {
        val currentWorkout = _currentWorkoutWithExercises.value
        val currentPos = _exercisePosition.value
        val total = _totalExercises.value
        
        if (currentWorkout != null && currentPos != -1 && total != -1 && currentPos < total) {
            val include = currentWorkout.workout.includeWarmupCooldown
            val assignments = currentWorkout.exerciseAssignments.sortedBy { it.assignment.order }
            val filtered = if (include) assignments else assignments.filter { it.assignment.category.equals("Exercise", true) }
            val nextAssignment = filtered.getOrNull(currentPos) 
            
            if (nextAssignment != null) {
                navigateToExerciseDetail(
                    workoutId = nextAssignment.assignment.workoutId,
                    exerciseId = nextAssignment.assignment.exerciseId,
                    category = nextAssignment.assignment.category,
                    position = currentPos + 1,
                    total = filtered.size
                )
            } else {
                navigateToWorkoutComplete()
            }
        } else if (currentPos != -1 && total != -1 && currentPos >= total) {
            navigateToWorkoutComplete()
        }
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
