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
import com.example.nutriority.data.repository.UserRepository
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
    private val userRepository: UserRepository,
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

    private val _activeDayIndex = MutableStateFlow(-1)
    val activeDayIndex = _activeDayIndex.asStateFlow()

    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    val elapsedTimeSeconds = _elapsedTimeSeconds.asStateFlow()

    private val _completedExercisesCount = MutableStateFlow(0)
    val completedExercisesCount = _completedExercisesCount.asStateFlow()

    // SESSION SUMMARY (Snapshot)
    data class SessionSummary(
        val workoutName: String, 
        val exercisesDone: Int, 
        val totalExercises: Int,
        val timeSeconds: Long, 
        val metValue: Double
    )
    private val _sessionSummary = MutableStateFlow<SessionSummary?>(null)
    val sessionSummary = _sessionSummary.asStateFlow()

    // Rest/Auto Log Timer State
    private val _isResting = MutableStateFlow(false)
    val isResting = _isResting.asStateFlow()

    private val _restTimeRemaining = MutableStateFlow(0L)
    val restTimeRemaining = _restTimeRemaining.asStateFlow()

    private val _isAutoLogActive = MutableStateFlow(false)
    val isAutoLogActive = _isAutoLogActive.asStateFlow()

    private val _autoLogSecondsRemaining = MutableStateFlow(0L)
    val autoLogSecondsRemaining = _autoLogSecondsRemaining.asStateFlow()

    private var workoutJob: Job? = null
    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private var autoLogTimerJob: Job? = null

    // Persistent Session Summary from DB
    val latestSessionLog = workoutRepository.getLatestSessionLog().asLiveData()

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

    fun startWorkout(workoutId: Int, dayIndex: Int = -1) {
        _isWorkoutActive.value = true
        _activeWorkoutId.value = workoutId
        _activeDayIndex.value = dayIndex
        _elapsedTimeSeconds.value = 0
        _sessionSummary.value = null
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
        val dayIdx = _activeDayIndex.value
        
        viewModelScope.launch {
            // 1. SAVE THE SESSION LOG
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

            // 2. UPDATE PERSONALIZED PROGRESS (CRITICAL)
            if (dayIdx != -1) {
                val user = userRepository.getInitialUser()
                if (user != null && dayIdx == user.lastCompletedWorkoutDay) {
                    userRepository.insertUser(user.copy(lastCompletedWorkoutDay = dayIdx + 1))
                }
            }
            
            // 3. CAPTURE SNAPSHOT for immediate UI use
            _sessionSummary.value = SessionSummary(
                workoutName = current.workout.name,
                exercisesDone = doneCount,
                totalExercises = totalCount,
                timeSeconds = timeSecs,
                metValue = current.workout.metValue
            )

            // 4. STOP SESSION
            stopWorkout(save = true)
        }
    }

    fun stopWorkout(save: Boolean) {
        val currentWorkoutId = _activeWorkoutId.value
        if (currentWorkoutId == -1) return
        
        _isWorkoutActive.value = false
        _activeWorkoutId.value = -1
        _activeDayIndex.value = -1
        timerJob?.cancel()
        _elapsedTimeSeconds.value = 0
        
        stopRestTimer()
        stopAutoLogTimer()
        
        viewModelScope.launch {
            val currentWorkout = _workout.value ?: return@launch
            workoutRepository.updateWorkoutWithExercises(
                currentWorkout.workout,
                currentWorkout.exerciseAssignments.map { it.assignment.copy(isCompleted = false) }
            )
            _completedExercisesCount.value = 0
        }
    }

    // Timer Methods
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedTimeSeconds.value += 1
            }
        }
    }

    fun startRestTimer(seconds: Long) {
        _restTimeRemaining.value = seconds
        _isResting.value = true
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (_restTimeRemaining.value > 0) {
                delay(1000)
                _restTimeRemaining.value -= 1
            }
            _isResting.value = false
        }
    }

    fun adjustRestTime(seconds: Long) {
        _restTimeRemaining.value = (_restTimeRemaining.value + seconds).coerceAtLeast(0)
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _isResting.value = false
        _restTimeRemaining.value = 0
    }

    fun startAutoLogTimer(seconds: Long, onComplete: () -> Unit) {
        _autoLogSecondsRemaining.value = seconds
        _isAutoLogActive.value = true
        autoLogTimerJob?.cancel()
        autoLogTimerJob = viewModelScope.launch {
            while (_autoLogSecondsRemaining.value > 0) {
                delay(1000)
                _autoLogSecondsRemaining.value -= 1
            }
            _isAutoLogActive.value = false
            onComplete()
        }
    }

    fun stopAutoLogTimer() {
        autoLogTimerJob?.cancel()
        _isAutoLogActive.value = false
        _autoLogSecondsRemaining.value = 0
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
