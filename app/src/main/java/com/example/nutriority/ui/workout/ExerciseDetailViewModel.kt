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

    fun getExerciseById(workoutId: Int, exerciseId: String) {
        viewModelScope.launch {
            workoutRepository.getWorkoutWithExercises(workoutId).collect { workoutWithExercises ->
                val assignment = workoutWithExercises?.exerciseAssignments?.find { it.assignment.exerciseId == exerciseId }
                _exerciseWithDetail.value = assignment
                _exercise.value = assignment?.exercise ?: workoutRepository.getExerciseById(exerciseId)
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
