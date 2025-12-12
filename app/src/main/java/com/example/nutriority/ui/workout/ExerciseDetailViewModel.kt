package com.example.nutriority.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Exercise
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

    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise = _exercise.asStateFlow()

    fun getExerciseById(exerciseId: Int) {
        viewModelScope.launch {
            _exercise.value = workoutRepository.getExerciseById(exerciseId)
        }
    }
}
