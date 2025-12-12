package com.example.nutriority.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
}
