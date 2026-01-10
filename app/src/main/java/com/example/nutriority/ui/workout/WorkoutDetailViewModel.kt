package com.example.nutriority.ui.workout

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.repository.WorkoutRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    suspend fun getDefaultExercisesFromAssets(workoutName: String): List<Exercise> {
        return try {
            val gson = Gson()
            val workoutJson = application.assets.open("workouts.json").bufferedReader().use(BufferedReader::readText)
            
            data class SimpleExercise(val name: String, val duration: String, val targetMuscle: String?)
            data class WorkoutJsonItem(val workout: Workout, val exercises: List<Exercise>, val warmup: List<SimpleExercise>, val cooldown: List<SimpleExercise>)
            
            val workoutType = object : TypeToken<List<WorkoutJsonItem>>() {}.type
            val workoutData: List<WorkoutJsonItem> = gson.fromJson(workoutJson, workoutType)
            
            // IMPROVED MATCHING: Look for name contains or ID match to be more robust
            val match = workoutData.find { 
                it.workout.name.equals(workoutName, ignoreCase = true) || 
                workoutName.contains(it.workout.name, ignoreCase = true) ||
                it.workout.name.contains(workoutName, ignoreCase = true)
            }
            
            if (match != null) {
                Log.d("WorkoutDetailVM", "Found default match for: $workoutName")
                val results = mutableListOf<Exercise>()
                match.warmup.forEach { w ->
                    results.add(Exercise(name = w.name, duration = w.duration, category = "Warm-up", targetMuscle = w.targetMuscle ?: ""))
                }
                match.exercises.forEach { e -> results.add(e.copy(category = "Exercise")) }
                match.cooldown.forEach { c ->
                    results.add(Exercise(name = c.name, duration = c.duration, category = "Cool-down", targetMuscle = c.targetMuscle ?: ""))
                }
                results
            } else {
                Log.e("WorkoutDetailVM", "No default workout found in JSON for name: $workoutName")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("WorkoutDetailVM", "Error reading assets for reset", e)
            emptyList()
        }
    }

    fun updateWorkout(workout: Workout, exercises: List<Exercise>) {
        viewModelScope.launch {
            workoutRepository.unlinkExercisesFromWorkout(workout.id)
            workoutRepository.updateWorkout(workout)
            val updatedExercises = exercises.map { it.copy(workoutId = workout.id) }
            workoutRepository.updateExercises(updatedExercises)
        }
    }
}
