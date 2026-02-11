package com.example.nutriority.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _exerciseWithDetail = MutableStateFlow<WorkoutExerciseWithDetail?>(null)
    val exerciseWithDetail = _exerciseWithDetail.asStateFlow()

    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise = _exercise.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun getExerciseById(workoutId: Int, exerciseId: String, category: String) {
        _isLoading.value = true
        viewModelScope.launch {
            // Fix: We observe the workout context reactively
            workoutRepository.getWorkoutExerciseWithDetail(workoutId, exerciseId, category)
                .distinctUntilChanged()
                .collectLatest { assignment ->
                    _exerciseWithDetail.value = assignment
                    
                    if (assignment != null) {
                        _exercise.value = assignment.exercise
                        _isLoading.value = false
                    } else {
                        // Fallback: If not found in workout context, check global library
                        val fallback = workoutRepository.getExerciseById(exerciseId)
                        if (fallback != null) {
                            _exercise.value = fallback
                        }
                        _isLoading.value = false
                    }
                }
        }
    }

    fun logWorkout(log: WorkoutLog) {
        viewModelScope.launch {
            workoutRepository.insertWorkoutLog(log)
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            workoutRepository.updateExercise(exercise)
        }
    }
}
