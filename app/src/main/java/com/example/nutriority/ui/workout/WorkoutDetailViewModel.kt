package com.example.nutriority.ui.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutSessionLog
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.repository.WorkoutRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    
    // Workout Session State
    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive = _isWorkoutActive.asStateFlow()

    private val _activeWorkoutId = MutableStateFlow(-1)
    val activeWorkoutId = _activeWorkoutId.asStateFlow()

    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    val elapsedTimeSeconds = _elapsedTimeSeconds.asStateFlow()

    private val _completedExercisesCount = MutableStateFlow(0)
    val completedExercisesCount = _completedExercisesCount.asStateFlow()

    // Persistent Session Summary from DB
    val latestSessionLog = workoutRepository.getLatestSessionLog().asLiveData()

    private var workoutJob: Job? = null
    private var timerJob: Job? = null

    private data class WorkoutExerciseJson(val exerciseId: String, val category: String?, val sets: Int, val reps: String, val rest: String, val duration: String?)
    private data class WorkoutJson(val id: Int, val name: String, val exercises: List<WorkoutExerciseJson>)
    private data class RootJson(val exercises: List<Exercise>, val workouts: List<WorkoutJson>)

    fun getWorkoutById(workoutId: Int) {
        if (_workout.value?.workout?.id == workoutId) return
        workoutJob?.cancel()
        _workout.value = null
        workoutJob = viewModelScope.launch {
            workoutRepository.getWorkoutWithExercises(workoutId).collect {
                _workout.value = it
                _completedExercisesCount.value = it.exerciseAssignments.count { it.assignment.isCompleted }
            }
        }
    }

    fun startWorkout(workoutId: Int) {
        _isWorkoutActive.value = true
        _activeWorkoutId.value = workoutId
        _elapsedTimeSeconds.value = 0
        startTimer()
    }

    fun resumeWorkout() {
        _isWorkoutActive.value = true
        startTimer()
    }

    fun pauseWorkout() {
        timerJob?.cancel()
    }

    fun finishWorkout() {
        val current = _workout.value ?: return
        val timeSecs = _elapsedTimeSeconds.value
        val doneCount = _completedExercisesCount.value
        val totalCount = current.exerciseAssignments.size
        
        viewModelScope.launch {
            // 1. SAVE THE SESSION
            val sessionLog = WorkoutSessionLog(
                workoutId = current.workout.id,
                workoutName = current.workout.name,
                date = System.currentTimeMillis(),
                exercisesDone = doneCount,
                totalExercises = totalCount,
                durationSeconds = timeSecs,
                caloriesBurned = (current.workout.metValue * 3.5 * 70 / 200 * (timeSecs / 60.0)).toInt(),
                difficulty = current.workout.difficulty
            )
            workoutRepository.insertSessionLog(sessionLog)
            
            // 2. DELAY RESET: Give the UI time to show the congratulatory screen before clearing
            delay(100) 
            
            // 3. STOP THE WORKOUT
            _isWorkoutActive.value = false
            _activeWorkoutId.value = -1
            timerJob?.cancel()
            _elapsedTimeSeconds.value = 0
            
            // Reset DB completion flags
            workoutRepository.updateWorkoutWithExercises(
                current.workout,
                current.exerciseAssignments.map { it.assignment.copy(isCompleted = false) }
            )
            _completedExercisesCount.value = 0
        }
    }

    fun stopWorkout(save: Boolean) {
        _isWorkoutActive.value = false
        _activeWorkoutId.value = -1
        timerJob?.cancel()
        _elapsedTimeSeconds.value = 0
        
        viewModelScope.launch {
            val currentWorkout = _workout.value ?: return@launch
            workoutRepository.updateWorkoutWithExercises(
                currentWorkout.workout,
                currentWorkout.exerciseAssignments.map { it.assignment.copy(isCompleted = false) }
            )
            _completedExercisesCount.value = 0
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedTimeSeconds.value += 1
            }
        }
    }

    fun formatElapsedTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
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

    fun updateExerciseCompletion(workoutId: Int, exerciseId: String, category: String, completed: Boolean) {
        if (_isWorkoutActive.value && _activeWorkoutId.value != workoutId) return
        viewModelScope.launch {
            workoutRepository.updateExerciseCompletion(workoutId, exerciseId, category, completed)
        }
    }

    fun updateWorkout(workout: Workout, workoutExercises: List<WorkoutExercise>) {
        viewModelScope.launch {
            workoutRepository.updateWorkoutWithExercises(workout, workoutExercises)
            _onWorkoutUpdated.emit(Unit)
        }
    }

    suspend fun getDefaultAssignmentsFromAssets(workoutId: Int, workoutName: String): List<WorkoutExercise> {
        return try {
            val gson = Gson()
            val jsonStr = application.assets.open("workouts.json").bufferedReader().use(BufferedReader::readText)
            val rootData = gson.fromJson(jsonStr, RootJson::class.java)
            val match = rootData.workouts.find { it.id == workoutId || it.name.equals(workoutName, ignoreCase = true) }
            match?.exercises?.mapIndexed { index, we ->
                WorkoutExercise(workoutId, we.exerciseId, we.category ?: "Exercise", we.sets, we.reps, we.rest, we.duration ?: "", index, false)
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}
