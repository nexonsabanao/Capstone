package com.example.nutriority.ui.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.repository.WorkoutRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val application: Application
) : AndroidViewModel(application) {

    private val _workout = MutableStateFlow<WorkoutWithExercises?>(null)
    val workout = _workout.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _onWorkoutUpdated = MutableSharedFlow<Unit>()
    val onWorkoutUpdated = _onWorkoutUpdated.asSharedFlow()
    
    private var workoutJob: Job? = null

    fun getWorkoutById(workoutId: Int) {
        if (_workout.value?.workout?.id == workoutId) return
        
        workoutJob?.cancel()
        _workout.value = null
        
        workoutJob = viewModelScope.launch {
            workoutRepository.getWorkoutWithExercises(workoutId).collect {
                _workout.value = it
            }
        }
    }

    fun updateWorkoutPreference(includeWarmupCooldown: Boolean) {
        val currentWorkout = _workout.value?.workout ?: return
        viewModelScope.launch {
            workoutRepository.updateWorkout(currentWorkout.copy(includeWarmupCooldown = includeWarmupCooldown))
        }
    }

    fun getAllExercises(): Flow<List<Exercise>> {
        return workoutRepository.getAllExercises()
    }

    private data class WorkoutExerciseJson(val exerciseId: String, val category: String?, val sets: Int, val reps: String, val rest: String, val duration: String?)
    private data class WorkoutJson(val id: Int, val name: String, val exercises: List<WorkoutExerciseJson>)
    private data class RootJson(val exercises: List<Exercise>, val workouts: List<WorkoutJson>)

    suspend fun getDefaultAssignmentsFromAssets(workoutId: Int, workoutName: String): List<WorkoutExercise> {
        return try {
            val gson = Gson()
            val jsonStr = application.assets.open("workouts.json").bufferedReader().use(BufferedReader::readText)
            val rootData: RootJson = gson.fromJson(jsonStr, RootJson::class.java)
            
            val match = rootData.workouts.find { it.id == workoutId || it.name.equals(workoutName, ignoreCase = true) }
            
            match?.exercises?.mapIndexed { index, we ->
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = we.exerciseId,
                    category = we.category ?: "Exercise",
                    sets = we.sets,
                    reps = we.reps,
                    rest = we.rest,
                    duration = we.duration ?: "",
                    order = index
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateWorkout(workout: Workout, workoutExercises: List<WorkoutExercise>) {
        viewModelScope.launch {
            workoutRepository.updateWorkoutWithExercises(workout, workoutExercises)
            _onWorkoutUpdated.emit(Unit)
        }
    }
}
