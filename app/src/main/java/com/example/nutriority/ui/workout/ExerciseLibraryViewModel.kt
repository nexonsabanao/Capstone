package com.example.nutriority.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _selectedTargetMuscle = MutableStateFlow("All")
    val selectedTargetMuscle: StateFlow<String> = _selectedTargetMuscle

    // Map primary muscle groups to all possible related keywords
    private val muscleGroupMap = mapOf(
        "Abs" to listOf("Abs", "Core", "Obliques"),
        "Shoulders" to listOf("Shoulders"),
        "Legs" to listOf("Legs", "Quads", "Hamstrings", "Glutes", "Calves"),
        "Back" to listOf("Back", "Lower Back", "Upper Back"),
        "Chest" to listOf("Chest", "Upper Chest", "Lower Chest"),
        "Arms" to listOf("Arms", "Biceps", "Triceps"),
        "Neck" to listOf("Neck")
    )

    val uniqueTargetMuscles: StateFlow<List<String>> = workoutRepository.getUniqueTargetMuscles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allExercises: StateFlow<List<Exercise>> = workoutRepository.getAllExercises()
        .combine(selectedTargetMuscle) { exercises, selectedFilter ->
            when {
                selectedFilter == "All" -> exercises
                selectedFilter == "Warm-up" || selectedFilter == "Cool-down" -> {
                    exercises.filter { it.category == selectedFilter }
                }
                else -> {
                    val keywords = muscleGroupMap[selectedFilter] ?: listOf(selectedFilter)
                    exercises.filter { exercise ->
                        keywords.any { keyword -> exercise.targetMuscle.contains(keyword) }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setTargetMuscle(targetMuscle: String) {
        _selectedTargetMuscle.value = targetMuscle
    }
}