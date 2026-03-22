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
import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import com.example.nutriority.data.repository.RecommendedWorkoutRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.planner.WorkoutSession
import com.example.nutriority.ui.util.WorkoutUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val recommendedWorkoutRepository: RecommendedWorkoutRepository,
    private val userRepository: UserRepository,
    private val application: Application
) : AndroidViewModel(application) {

    // Currently VIEWED workout
    private val _workout = MutableStateFlow<WorkoutWithExercises?>(null)
    val workout = _workout.asStateFlow()

    // Currently ACTIVE workout (The one that is started/running)
    private val _activeWorkoutDetail = MutableStateFlow<WorkoutWithExercises?>(null)
    val activeWorkoutDetail = _activeWorkoutDetail.asStateFlow()

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

    data class SessionSummary(
        val workoutName: String, 
        val exercisesDone: Int, 
        val totalExercises: Int,
        val timeSeconds: Long, 
        val caloriesBurned: Int,
        val difficulty: String
    )
    private val _sessionSummary = MutableStateFlow<SessionSummary?>(null)
    val sessionSummary = _sessionSummary.asStateFlow()

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

    val latestSessionLog = workoutRepository.getLatestSessionLog().asLiveData()

    fun resolveWorkout(workoutId: Int, session: WorkoutSession? = null) {
        workoutJob?.cancel()
        _isLoading.value = true
        _workout.value = null
        
        workoutJob = viewModelScope.launch {
            if (session != null) {
                // If session is provided (Personalized workout), ensure it's synced to DB first
                val existing = workoutRepository.getWorkoutWithExercises(workoutId).first()
                syncSessionToDb(workoutId, session, existing?.workout?.includeWarmupCooldown ?: true)
            }

            // Use collectLatest on the repository flow to handle real-time updates (like exercise completion)
            // This fixes the bug where personalized workouts didn't update the UI when an exercise was done.
            workoutRepository.getWorkoutWithExercises(workoutId)
                .distinctUntilChanged()
                .collectLatest { detail ->
                    if (detail != null) {
                        // CRITICAL: Clean duration data on load
                        val safeDuration = WorkoutUtil.calculateTotalDuration(detail.exerciseAssignments, detail.workout.includeWarmupCooldown)
                        if (detail.workout.duration != safeDuration) {
                            detail.workout.duration = safeDuration
                            launch { workoutRepository.updateWorkout(detail.workout) }
                        }
                    }

                    _workout.value = detail
                    
                    if (_isWorkoutActive.value && _activeWorkoutId.value == workoutId) {
                        _activeWorkoutDetail.value = detail
                    }
                    
                    _completedExercisesCount.value = detail?.exerciseAssignments?.count { it.assignment.isCompleted } ?: 0
                    _isLoading.value = false
                }
        }
    }

    private suspend fun mapPlannerToDetail(workoutId: Int, pe: com.example.nutriority.planner.PlannerExercise, category: String): WorkoutExerciseWithDetail {
        val exercise = workoutRepository.getExerciseById(pe.exerciseId) ?: Exercise(id = pe.exerciseId, name = pe.name)
        val assignment = WorkoutExercise(
            workoutId = workoutId,
            exerciseId = pe.exerciseId,
            category = category,
            sets = pe.sets,
            reps = pe.reps,
            duration = pe.duration,
            rest = pe.rest,
            isCompleted = false
        )
        return WorkoutExerciseWithDetail(assignment, exercise)
    }

    private suspend fun syncSessionToDb(workoutId: Int, session: WorkoutSession, includeWarmup: Boolean) {
        // Fetch existing assignments to preserve completion status
        val existingWorkout = workoutRepository.getWorkoutWithExercises(workoutId).first()
        val existingAssignments = existingWorkout?.exerciseAssignments ?: emptyList()

        val assignments = mutableListOf<WorkoutExercise>()
        var order = 0
        
        fun getCompletedStatus(exId: String, cat: String): Boolean {
            return existingAssignments.find { it.assignment.exerciseId == exId && it.assignment.category == cat }?.assignment?.isCompleted ?: false
        }

        session.warmup?.forEach { ex -> 
            assignments.add(createWorkoutExercise(workoutId, ex, "warmup", order++, getCompletedStatus(ex.exerciseId, "warmup"))) 
        }
        session.exercises?.forEach { ex -> 
            assignments.add(createWorkoutExercise(workoutId, ex, "Exercise", order++, getCompletedStatus(ex.exerciseId, "Exercise"))) 
        }
        session.cooldown?.forEach { ex -> 
            assignments.add(createWorkoutExercise(workoutId, ex, "cooldown", order++, getCompletedStatus(ex.exerciseId, "cooldown"))) 
        }
        
        // Calculate duration properly instead of relying on the planner's estimate
        val tempDetails = mutableListOf<WorkoutExerciseWithDetail>()
        for (a in assignments) {
            val ex = workoutRepository.getExerciseById(a.exerciseId) ?: Exercise(id = a.exerciseId, name = "Unknown")
            tempDetails.add(WorkoutExerciseWithDetail(a, ex))
        }
        val safeDuration = WorkoutUtil.calculateTotalDuration(tempDetails, includeWarmup)

        // Set MET value based on difficulty for more accurate calorie burn
        val metValue = when {
            session.focus.contains("Advanced", ignoreCase = true) -> 8.0
            session.focus.contains("Beginner", ignoreCase = true) -> 4.0
            else -> 6.0 // Intermediate
        }

        val workout = Workout(
            id = workoutId,
            name = session.focus,
            description = session.description,
            category = "Personalized",
            difficulty = if (session.focus.contains("Beginner")) "Beginner" else if (session.focus.contains("Advanced")) "Advanced" else "Intermediate",
            duration = safeDuration,
            imageName = "img_gym_bg",
            includeWarmupCooldown = includeWarmup,
            metValue = metValue
        )
        workoutRepository.updateWorkoutWithExercises(workout, assignments)
    }

    private fun createWorkoutExercise(
        workoutId: Int, 
        pe: com.example.nutriority.planner.PlannerExercise, 
        category: String, 
        order: Int,
        isCompleted: Boolean = false
    ): WorkoutExercise {
        return WorkoutExercise(
            workoutId = workoutId,
            exerciseId = pe.exerciseId,
            category = category,
            sets = pe.sets,
            reps = pe.reps,
            duration = pe.duration,
            rest = pe.rest,
            order = order,
            isCompleted = isCompleted
        )
    }

    fun getWorkoutById(workoutId: Int) {
        resolveWorkout(workoutId, null)
    }

    fun startWorkout(workoutId: Int, dayIndex: Int = -1) {
        _isWorkoutActive.value = true
        _activeWorkoutId.value = workoutId
        _activeDayIndex.value = dayIndex
        _elapsedTimeSeconds.value = 0
        _sessionSummary.value = null
        
        _activeWorkoutDetail.value = _workout.value
        
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
        finishWorkoutWithWeight(null)
    }

    fun finishWorkoutWithWeight(weightKg: Double?) {
        val current = _activeWorkoutDetail.value ?: _workout.value ?: return
        val timeSecs = _elapsedTimeSeconds.value
        val doneCount = _completedExercisesCount.value
        val totalCount = current.exerciseAssignments.size
        val dayIdx = _activeDayIndex.value
        
        viewModelScope.launch {
            val user = userRepository.getInitialUser()
            // Use provided weight, then user's saved weight, then fallback to 70kg
            val finalWeight = weightKg ?: user?.weightKg ?: 70.0
            
            // Formula: Calories = (MET * 3.5 * weightKg / 200) * durationInMinutes
            val durationMinutes = timeSecs / 60.0
            val cals = (current.workout.metValue * 3.5 * finalWeight / 200.0 * durationMinutes).toInt()

            // Update user weight if a weight was provided
            if (weightKg != null && user != null) {
                userRepository.insertUser(user.copy(weightKg = weightKg))
            }

            val now = System.currentTimeMillis()
            val targetCal = Calendar.getInstance().apply { timeInMillis = now }
            val existingLogs = workoutRepository.getAllSessionLogs().first()
            val existingTodayLog = existingLogs.find { 
                val logCal = Calendar.getInstance().apply { timeInMillis = it.date }
                logCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                logCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR) &&
                it.workoutId == current.workout.id
            }

            val sessionLog = if (existingTodayLog != null) {
                existingTodayLog.copy(
                    exercisesDone = doneCount,
                    totalExercises = totalCount,
                    durationSeconds = timeSecs,
                    caloriesBurned = cals,
                    weightKg = finalWeight,
                    date = now 
                )
            } else {
                WorkoutSessionLog(
                    workoutId = current.workout.id,
                    workoutName = current.workout.name,
                    date = now,
                    exercisesDone = doneCount,
                    totalExercises = totalCount,
                    durationSeconds = timeSecs,
                    caloriesBurned = cals,
                    difficulty = current.workout.difficulty,
                    weightKg = finalWeight
                )
            }
            
            workoutRepository.insertSessionLog(sessionLog)

            if (dayIdx != -1 && user != null) {
                if (dayIdx == user.lastCompletedWorkoutDay) {
                    userRepository.insertUser(user.copy(lastCompletedWorkoutDay = dayIdx + 1))
                }
            }
            
            _sessionSummary.value = SessionSummary(
                workoutName = current.workout.name,
                exercisesDone = doneCount,
                totalExercises = totalCount,
                timeSeconds = timeSecs,
                caloriesBurned = cals,
                difficulty = current.workout.difficulty
            )

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
            val currentWorkout = _activeWorkoutDetail.value ?: return@launch
            
            val resetAssignments = currentWorkout.exerciseAssignments.map { 
                it.assignment.copy(isCompleted = false) 
            }
            workoutRepository.updateWorkoutWithExercises(currentWorkout.workout, resetAssignments)
            
            _completedExercisesCount.value = 0
            _activeWorkoutDetail.value = null
        }
    }

    fun deleteWorkout(workout: Workout) {
        viewModelScope.launch {
            workoutRepository.deleteFullWorkout(workout)
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
        val current = _workout.value ?: return
        val updatedWorkout = current.workout.copy(includeWarmupCooldown = includeWarmupCooldown)
        
        // Immediate UI update
        _workout.value = current.copy(workout = updatedWorkout)
        
        viewModelScope.launch {
            // Persist to DB
            workoutRepository.updateWorkout(updatedWorkout)
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
            
            val workoutWithDetails = workoutRepository.getWorkoutWithExercises(workout.id).first()
            if (workoutWithDetails != null) {
                val newDuration = WorkoutUtil.calculateTotalDuration(
                    workoutWithDetails.exerciseAssignments, 
                    workout.includeWarmupCooldown
                )
                workoutRepository.updateWorkout(workout.copy(duration = newDuration))
            }
            _onWorkoutUpdated.emit(Unit)
        }
    }

    fun updateWorkoutExercise(workoutExercise: WorkoutExercise) {
        viewModelScope.launch {
            workoutRepository.updateWorkoutExercise(workoutExercise)
        }
    }

    suspend fun getOriginalAssignments(workoutId: Int): List<WorkoutExercise> {
        return recommendedWorkoutRepository.getOriginalAssignments(workoutId)
    }
}
