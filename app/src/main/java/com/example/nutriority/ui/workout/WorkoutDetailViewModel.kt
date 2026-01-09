package com.example.nutriority.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _workout = MutableStateFlow<WorkoutWithExercises?>(null)
    val workout = _workout.asStateFlow()

    fun getWorkoutById(workoutId: Int) {
        viewModelScope.launch {
            workoutRepository.getWorkoutWithExercises(workoutId).collect {
                _workout.value = it
            }
        }
    }

    fun updateExercises(exercises: List<Exercise>) {
        viewModelScope.launch {
            workoutRepository.updateExercises(exercises)
        }
    }

    fun getAllExercises(): Flow<List<Exercise>> {
        return workoutRepository.getAllExercises()
    }

    fun updateWorkout(workout: Workout, exercises: List<Exercise>) {
        viewModelScope.launch {
            // Step 1: Unlink ALL currently linked exercises for this workout
            workoutRepository.unlinkExercisesFromWorkout(workout.id)
            
            // Step 2: Update the workout (e.g., its name)
            workoutRepository.updateWorkout(workout)
            
            // Step 3: Link only the NEWLY selected exercises
            val updatedExercises = exercises.map { it.copy(workoutId = workout.id) }
            workoutRepository.updateExercises(updatedExercises)
        }
    }
}
