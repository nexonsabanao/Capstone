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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val workoutLogDao: WorkoutLogDao,
    private val application: Application
) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allWorkouts: Flow<List<Workout>> = workoutDao.getAllWorkouts()

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

    fun startRealtimeExerciseSync(scope: CoroutineScope) {
        db.collection("exercises").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("WorkoutRepo", "Exercise sync failed", e)
                return@addSnapshotListener
            }
            
            snapshot?.let {
                val cloudExercises = it.toObjects(Exercise::class.java)
                scope.launch(Dispatchers.IO) {
                    if (cloudExercises.isNotEmpty()) {
                        cloudExercises.forEachIndexed { index, exercise ->
                            if (exercise.id.isEmpty()) {
                                exercise.id = it.documents[index].id
                            }
                        }
                        // Use upsert instead of replace to protect foreign keys
                        workoutDao.upsertExercises(cloudExercises)
                        Log.d("WorkoutRepo", "Real-time exercise sync: ${cloudExercises.size} updated")
                    }
                }
            }
        }
    }

    fun startRealtimeCustomWorkoutSync(scope: CoroutineScope) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("custom_workouts")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                snapshot?.let {
                    scope.launch(Dispatchers.IO) {
                        val exercises = workoutDao.getAllExercises().first()
                        if (exercises.isEmpty()) return@launch 
                        
                        val existingIds = exercises.map { it.id }.toSet()
                        it.documents.forEach { doc ->
                            val dto = doc.toObject(CustomWorkoutDto::class.java) ?: return@forEach
                            val workout = dto.workout ?: return@forEach
                            val assignments = dto.exercises.filter { existingIds.contains(it.exerciseId) }
                            
                            if (assignments.isNotEmpty() || dto.exercises.isEmpty()) {
                                workoutDao.updateWorkoutWithExercises(workout, assignments)
                            }
                        }
                    }
                }
            }
    }

    suspend fun syncExercisesFromCloud() {
        try {
            val snapshot = db.collection("exercises").get().await()
            val cloud = snapshot.toObjects(Exercise::class.java)
            if (cloud.isNotEmpty()) {
                cloud.forEachIndexed { i, ex -> if (ex.id.isEmpty()) ex.id = snapshot.documents[i].id }
                workoutDao.upsertExercises(cloud)
            }
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "Sync exercises error: ${e.message}")
        }
    }

    suspend fun ensureLibraryIsLoaded() {
        if (workoutDao.getExerciseCount() == 0) syncExercisesFromCloud()
    }

    private suspend fun syncCustomWorkoutToCloud(workoutId: Int) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val workout = workoutDao.getWorkoutById(workoutId)
            val detail = workoutDao.getWorkoutWithExercises(workoutId).first()
            if (workout != null && detail != null) {
                val dto = CustomWorkoutDto(workout, detail.exerciseAssignments.map { it.assignment })
                db.collection("users").document(uid).collection("custom_workouts").document(workoutId.toString()).set(dto).await()
            }
        } catch (e: Exception) { }
    }

    suspend fun deleteFullWorkout(workout: Workout) {
        workoutDao.deleteFullWorkout(workout)
        val uid = auth.currentUser?.uid ?: return
        repositoryScope.launch {
            try { db.collection("users").document(uid).collection("custom_workouts").document(workout.id.toString()).delete().await() } catch (e: Exception) { }
        }
    }

    suspend fun insertWorkoutLog(log: WorkoutLog) {
        workoutLogDao.insertLog(log)
        val uid = auth.currentUser?.uid ?: return
        repositoryScope.launch {
            try { db.collection("users").document(uid).collection("exercise_records").add(log).await() } catch (e: Exception) { }
        }
    }

    suspend fun insertSessionLog(log: WorkoutSessionLog) {
        workoutDao.insertSessionLog(log)
        val uid = auth.currentUser?.uid ?: return
        repositoryScope.launch {
            try { db.collection("users").document(uid).collection("session_history").add(log).await() } catch (e: Exception) { }
        }
    }

    suspend fun restoreHistoryFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        try {
            ensureLibraryIsLoaded()
            val existingIds = workoutDao.getAllExercises().first().map { it.id }.toSet()
            
            val customWorkouts = db.collection("users").document(uid).collection("custom_workouts").get().await()
            customWorkouts.documents.forEach { doc ->
                val dto = doc.toObject(CustomWorkoutDto::class.java) ?: return@forEach
                val workout = dto.workout ?: return@forEach
                val assignments = dto.exercises.filter { existingIds.contains(it.exerciseId) }
                if (assignments.isNotEmpty() || dto.exercises.isEmpty()) {
                    workoutDao.updateWorkoutWithExercises(workout, assignments)
                }
            }

            val sessions = db.collection("users").document(uid).collection("session_history").get().await()
            sessions.documents.forEach { doc ->
                try {
                    val log = WorkoutSessionLog(
                        workoutId = (doc.get("workoutId") as? Number)?.toInt() ?: 0,
                        workoutName = doc.get("workoutName") as? String ?: "",
                        date = (doc.get("date") as? Number)?.toLong() ?: 0L,
                        exercisesDone = (doc.get("exercisesDone") as? Number)?.toInt() ?: 0,
                        totalExercises = (doc.get("totalExercises") as? Number)?.toInt() ?: 0,
                        durationSeconds = (doc.get("durationSeconds") as? Number)?.toLong() ?: 0L,
                        caloriesBurned = (doc.get("caloriesBurned") as? Number)?.toInt() ?: 0,
                        difficulty = doc.get("difficulty") as? String ?: "Intermediate",
                        weightKg = (doc.get("weightKg") as? Number)?.toDouble() ?: 0.0
                    )
                    workoutDao.insertSessionLog(log)
                } catch (e: Exception) { }
            }

            val records = db.collection("users").document(uid).collection("exercise_records").get().await()
            records.documents.forEach { doc ->
                try {
                    val timestamp = doc.get("date") as? Timestamp
                    val log = WorkoutLog(
                        workoutId = (doc.get("workoutId") as? Number)?.toInt() ?: 0,
                        exerciseName = doc.get("exerciseName") as? String ?: "",
                        date = timestamp?.toDate() ?: Date(),
                        reps = doc.get("reps") as? String ?: "",
                        weightKg = (doc.get("weightKg") as? Number)?.toDouble() ?: 0.0
                    )
                    workoutLogDao.insertLog(log)
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }
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
    fun getUniqueTargetMuscles(): Flow<List<String>> = workoutDao.getAllExercises().map { ex -> ex.map { it.target }.filter { it.isNotBlank() }.distinct().sorted() }
    suspend fun getExercisesByFocusAndDifficulty(f: String, d: String): List<Exercise> = workoutDao.getExercisesByFocusAndDifficulty(f, d)
    suspend fun getExercisesByFocus(f: String): List<Exercise> = workoutDao.getExercisesByFocus(f)
}
