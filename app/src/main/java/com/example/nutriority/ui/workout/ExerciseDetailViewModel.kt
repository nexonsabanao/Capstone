package com.example.nutriority.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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

    // Rest Timer State
    private val _restTimeRemaining = MutableStateFlow(0L)
    val restTimeRemaining = _restTimeRemaining.asStateFlow()

    private val _isResting = MutableStateFlow(false)
    val isResting = _isResting.asStateFlow()

    // Auto Log Timer State
    private val _autoLogSecondsRemaining = MutableStateFlow(0L)
    val autoLogSecondsRemaining = _autoLogSecondsRemaining.asStateFlow()

    private val _isAutoLogActive = MutableStateFlow(false)
    val isAutoLogActive = _isAutoLogActive.asStateFlow()

    private var restTimerJob: Job? = null
    private var autoLogTimerJob: Job? = null

    fun getExerciseById(workoutId: Int, exerciseId: String) {
        viewModelScope.launch {
            workoutRepository.getWorkoutWithExercises(workoutId).collect { workoutWithExercises ->
                val assignment = workoutWithExercises.exerciseAssignments.find { it.assignment.exerciseId == exerciseId }
                _exerciseWithDetail.value = assignment
                _exercise.value = assignment?.exercise ?: workoutRepository.getExerciseById(exerciseId)
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

    fun adjustRestTime(seconds: Long) {
        _restTimeRemaining.value = (_restTimeRemaining.value + seconds).coerceAtLeast(0)
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _isResting.value = false
        _restTimeRemaining.value = 0
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
