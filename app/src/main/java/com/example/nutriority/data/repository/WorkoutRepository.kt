package com.example.nutriority.data.repository

import android.app.Application
import android.util.Log
import com.example.nutriority.data.local.WorkoutDao
import com.example.nutriority.data.local.WorkoutLogDao
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.data.model.WorkoutSessionLog
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val workoutLogDao: WorkoutLogDao,
    private val application: Application
) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allWorkouts: Flow<List<Workout>> = workoutDao.getAllWorkouts()

    // Helper data class for Firestore structure
    data class CustomWorkoutDto(
        val workout: Workout? = null,
        val exercises: List<WorkoutExercise> = emptyList()
    )

    suspend fun getAllWorkoutsList(): List<WorkoutWithExercises> {
        return workoutDao.getAllWorkoutsWithExercises().first()
    }

    fun getWorkoutWithExercises(workoutId: Int): Flow<WorkoutWithExercises?> {
        return workoutDao.getWorkoutWithExercises(workoutId)
    }

    fun getWorkoutExerciseWithDetail(workoutId: Int, exerciseId: String, category: String): Flow<WorkoutExerciseWithDetail?> {
        return workoutDao.getWorkoutExerciseWithDetail(workoutId, exerciseId, category)
    }

    suspend fun getExerciseById(exerciseId: String): Exercise? {
        return workoutDao.getExerciseById(exerciseId)
    }

    suspend fun updateExercise(exercise: Exercise) {
        workoutDao.updateExercise(exercise)
    }

    suspend fun updateWorkout(workout: Workout) {
        workoutDao.updateWorkout(workout)
    }

    suspend fun updateExerciseCompletion(workoutId: Int, exerciseId: String, category: String, completed: Boolean) {
        workoutDao.updateExerciseCompletion(workoutId, exerciseId, category, completed)
    }

    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise) {
        workoutDao.updateWorkoutExercise(workoutExercise)
        repositoryScope.launch {
            syncCustomWorkoutToCloud(workoutExercise.workoutId)
        }
    }

    suspend fun updateWorkoutWithExercises(workout: Workout, workoutExercises: List<WorkoutExercise>) {
        workoutDao.updateWorkoutWithExercises(workout, workoutExercises)
        repositoryScope.launch {
            syncCustomWorkoutToCloud(workout.id)
        }
    }

    suspend fun updateWorkoutsWithExercises(workouts: List<Workout>, workoutExercises: List<WorkoutExercise>) {
        workoutDao.updateWorkoutsWithExercises(workouts, workoutExercises)
    }

    suspend fun syncExercisesFromCloud() {
        try {
            val exerciseSnapshot = db.collection("exercises").get().await()
            val cloudExercises = exerciseSnapshot.toObjects(Exercise::class.java)
            if (cloudExercises.isNotEmpty()) {
                cloudExercises.forEachIndexed { index, exercise ->
                    if (exercise.id.isEmpty()) {
                        exercise.id = exerciseSnapshot.documents[index].id
                    }
                }
                workoutDao.insertAllExercises(cloudExercises)
                Log.d("WorkoutRepo", "Synced ${cloudExercises.size} exercises from Firestore")
            }
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Error syncing exercises: ${e.message}")
        }
    }

    suspend fun ensureLibraryIsLoaded() {
        val count = workoutDao.getExerciseCount()
        if (count == 0) {
            syncExercisesFromCloud()
        }
    }

    private suspend fun syncCustomWorkoutToCloud(workoutId: Int) {
        val uid = auth.currentUser?.uid ?: return
        if (workoutId >= 1000) return 
        
        try {
            val workout = workoutDao.getWorkoutById(workoutId)
            val detail = workoutDao.getWorkoutWithExercises(workoutId).first()
            if (workout != null && detail != null) {
                val dto = CustomWorkoutDto(
                    workout = workout,
                    exercises = detail.exerciseAssignments.map { it.assignment }
                )
                db.collection("users").document(uid)
                    .collection("custom_workouts")
                    .document(workoutId.toString())
                    .set(dto).await()
            }
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Error syncing custom workout: ${e.message}")
        }
    }

    suspend fun deleteFullWorkout(workout: Workout) {
        workoutDao.deleteFullWorkout(workout)
        val uid = auth.currentUser?.uid ?: return
        repositoryScope.launch {
            try {
                db.collection("users").document(uid)
                    .collection("custom_workouts")
                    .document(workout.id.toString())
                    .delete().await()
            } catch (e: Exception) { }
        }
    }

    suspend fun insertWorkoutLog(log: WorkoutLog) {
        workoutLogDao.insertLog(log)
        val uid = auth.currentUser?.uid ?: return
        repositoryScope.launch {
            try {
                // Using a map for specific date formatting if needed, but object works too
                db.collection("users").document(uid).collection("exercise_records").add(log).await()
            } catch (e: Exception) { }
        }
    }

    suspend fun insertSessionLog(log: WorkoutSessionLog) {
        workoutDao.insertSessionLog(log)
        val uid = auth.currentUser?.uid ?: return
        repositoryScope.launch {
            try {
                db.collection("users").document(uid).collection("session_history").add(log).await()
            } catch (e: Exception) { }
        }
    }

    suspend fun restoreHistoryFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        try {
            ensureLibraryIsLoaded()
            val existingExerciseIds = workoutDao.getAllExercises().first().map { it.id }.toSet()
            
            // Restore Custom Workouts
            val customWorkouts = db.collection("users").document(uid).collection("custom_workouts").get().await()
            customWorkouts.documents.forEach { doc ->
                val dto = doc.toObject(CustomWorkoutDto::class.java) ?: return@forEach
                val workout = dto.workout ?: return@forEach
                val assignments = dto.exercises.filter { existingExerciseIds.contains(it.exerciseId) }
                
                workoutDao.updateWorkoutWithExercises(workout, assignments)
            }

            // Restore Session History
            val sessions = db.collection("users").document(uid).collection("session_history").get().await()
            val sessionLogs = sessions.toObjects(WorkoutSessionLog::class.java)
            sessionLogs.forEach { workoutDao.insertSessionLog(it) }

            // Restore Exercise Records
            val records = db.collection("users").document(uid).collection("exercise_records").get().await()
            val exerciseLogs = records.toObjects(WorkoutLog::class.java)
            exerciseLogs.forEach { workoutLogDao.insertLog(it) }
            
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Restore error: ${e.message}")
        }
    }

    suspend fun deleteAllHistory() {
        workoutLogDao.deleteAll()
        workoutDao.deleteAllSessionLogs()
        workoutDao.deleteAllCustomWorkouts()
    }

    fun getLatestSessionLog(): Flow<WorkoutSessionLog?> = workoutDao.getLatestSessionLog()
    fun getAllSessionLogs(): Flow<List<WorkoutSessionLog>> = workoutDao.getAllSessionLogs()
    fun getWorkoutLogs(): Flow<List<WorkoutLog>> = workoutLogDao.getWorkoutLogs()
    fun getAllExercises(): Flow<List<Exercise>> = workoutDao.getAllExercises()
    
    fun getUniqueTargetMuscles(): Flow<List<String>> = workoutDao.getAllExercises().map { exercises -> 
        exercises.map { it.target }.filter { it.isNotBlank() }.distinct().sorted() 
    }
    
    suspend fun getExercisesByFocusAndDifficulty(focus: String, difficulty: String): List<Exercise> {
        return workoutDao.getExercisesByFocusAndDifficulty(focus, difficulty)
    }

    suspend fun getExercisesByFocus(focus: String): List<Exercise> {
        return workoutDao.getExercisesByFocus(focus)
    }
}
