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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.BufferedReader

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val workoutLogDao: WorkoutLogDao,
    private val application: Application
) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val allWorkouts: Flow<List<Workout>> = workoutDao.getAllWorkouts().map { workouts ->
        workouts.map { workout ->
            workout.copy().apply {
                imageResId = application.resources.getIdentifier(imageName, "drawable", application.packageName)
            }
        }
    }

    suspend fun getAllWorkoutsList(): List<WorkoutWithExercises> {
        return workoutDao.getAllWorkoutsWithExercises().first().map { it.applyImages() }
    }

    fun getWorkoutWithExercises(workoutId: Int): Flow<WorkoutWithExercises> {
        return workoutDao.getWorkoutWithExercises(workoutId).map { it.applyImages() }
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
        if (workoutExercise.workoutId > 25) {
            syncCustomWorkoutToCloud(workoutExercise.workoutId)
        }
    }

    suspend fun updateWorkoutWithExercises(workout: Workout, workoutExercises: List<WorkoutExercise>) {
        workoutDao.updateWorkoutWithExercises(workout, workoutExercises)
        if (workout.id > 25) syncCustomWorkoutToCloud(workout.id)
    }

    /**
     * MANDATORY RESTORE: Loads official exercises and trainer workouts from assets.
     */
    suspend fun ensureLibraryIsLoaded() {
        val exerciseCount = workoutDao.getExerciseCount()
        if (exerciseCount > 0) return
        
        try {
            val jsonStr = application.assets.open("workouts.json").bufferedReader().use(BufferedReader::readText)
            val root = Gson().fromJson(jsonStr, RootJson::class.java)
            
            // 1. Load all base exercises
            root.exercises.forEach { workoutDao.insertExercise(it) }
            
            // 2. Load official trainer workouts (ID 1-25)
            root.workouts.forEach { w ->
                val workout = Workout(
                    id = w.id, 
                    name = w.name, 
                    description = w.description,
                    category = w.category, 
                    targetMuscle = w.targetMuscle,
                    imageName = w.imageName,
                    difficulty = w.difficulty,
                    duration = w.duration,
                    metValue = if (w.category.lowercase().contains("cardio")) 8.0 else 5.0
                )
                val assignments = w.exercises.mapIndexed { index, we ->
                    WorkoutExercise(w.id, we.exerciseId, we.category ?: "Exercise", we.sets, we.reps, we.rest, we.duration ?: "", index)
                }
                workoutDao.updateWorkoutWithExercises(workout, assignments)
            }
            Log.d("Restore", "Workout Library Loaded Successfully")
        } catch (e: Exception) {
            Log.e("Restore", "Failed to load library", e)
        }
    }

    private suspend fun syncCustomWorkoutToCloud(workoutId: Int) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val workout = workoutDao.getWorkoutById(workoutId)
            val detail = workoutDao.getWorkoutWithExercises(workoutId).first()
            if (workout != null) {
                val workoutMap = hashMapOf(
                    "workout" to hashMapOf(
                        "id" to workout.id,
                        "name" to workout.name,
                        "description" to workout.description,
                        "category" to workout.category,
                        "targetMuscle" to workout.targetMuscle,
                        "imageName" to workout.imageName,
                        "difficulty" to workout.difficulty,
                        "duration" to workout.duration,
                        "metValue" to workout.metValue,
                        "includeWarmupCooldown" to workout.includeWarmupCooldown
                    ),
                    "exercises" to detail.exerciseAssignments.map { 
                        val assignment = it.assignment
                        hashMapOf(
                            "workoutId" to assignment.workoutId,
                            "exerciseId" to assignment.exerciseId,
                            "category" to assignment.category,
                            "sets" to assignment.sets,
                            "reps" to assignment.reps,
                            "rest" to assignment.rest,
                            "duration" to assignment.duration,
                            "order" to assignment.order,
                            "isCompleted" to assignment.isCompleted
                        )
                    }
                )
                db.collection("users").document(uid).collection("custom_workouts").document(workoutId.toString()).set(workoutMap).await()
            }
        } catch (e: Exception) { }
    }

    suspend fun deleteFullWorkout(workout: Workout) {
        workoutDao.deleteFullWorkout(workout)
        auth.currentUser?.uid?.let { uid ->
            try {
                db.collection("users").document(uid).collection("custom_workouts").document(workout.id.toString()).delete().await()
            } catch (e: Exception) { }
        }
    }

    suspend fun insertWorkoutLog(log: WorkoutLog) {
        workoutLogDao.insertLog(log)
        auth.currentUser?.uid?.let { uid ->
            try {
                val logMap = hashMapOf("workoutId" to log.workoutId, "date" to Timestamp(log.date), "reps" to log.reps, "weightKg" to log.weightKg)
                db.collection("users").document(uid).collection("exercise_records").add(logMap).await()
            } catch (e: Exception) { }
        }
    }

    suspend fun insertSessionLog(log: WorkoutSessionLog) {
        workoutDao.insertSessionLog(log)
        auth.currentUser?.uid?.let { uid ->
            try {
                val logMap = hashMapOf("workoutId" to log.workoutId, "workoutName" to log.workoutName, "date" to log.date, "exercisesDone" to log.exercisesDone, "totalExercises" to log.totalExercises, "durationSeconds" to log.durationSeconds, "caloriesBurned" to log.caloriesBurned, "difficulty" to log.difficulty, "weightKg" to log.weightKg)
                db.collection("users").document(uid).collection("session_history").add(logMap).await()
            } catch (e: Exception) { }
        }
    }

    suspend fun restoreHistoryFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        try {
            ensureLibraryIsLoaded()
            val existingExerciseIds = workoutDao.getAllExercises().first().map { it.id }.toSet()
            val customWorkouts = db.collection("users").document(uid).collection("custom_workouts").get().await()
            customWorkouts.documents.forEach { doc ->
                val wData = doc.get("workout") as? Map<*, *> ?: return@forEach
                @Suppress("UNCHECKED_CAST")
                val exData = doc.get("exercises") as? List<Map<String, Any>> ?: emptyList()
                
                val workout = Workout(id = (wData["id"] as? Number)?.toInt() ?: 0, name = wData["name"] as? String ?: "", description = wData["description"] as? String ?: "", category = wData["category"] as? String ?: "Custom", targetMuscle = wData["targetMuscle"] as? String ?: "", imageName = wData["imageName"] as? String ?: "", difficulty = wData["difficulty"] as? String ?: "Intermediate", duration = wData["duration"] as? String ?: "", metValue = (wData["metValue"] as? Number)?.toDouble() ?: 5.0, includeWarmupCooldown = wData["includeWarmupCooldown"] as? Boolean ?: false)
                
                val assignments = exData.map { ex ->
                    WorkoutExercise(workoutId = (ex["workoutId"] as? Number)?.toInt() ?: workout.id, exerciseId = ex["exerciseId"] as? String ?: "", category = ex["category"] as? String ?: "Exercise", sets = (ex["sets"] as? Number)?.toInt() ?: 3, reps = ex["reps"] as? String ?: "10", rest = ex["rest"] as? String ?: "60s", duration = ex["duration"] as? String ?: "", order = (ex["order"] as? Number)?.toInt() ?: 0)
                }.filter { existingExerciseIds.contains(it.exerciseId) }
                
                workoutDao.updateWorkoutWithExercises(workout, assignments)
            }
            val sessions = db.collection("users").document(uid).collection("session_history").get().await()
            sessions.documents.forEach { doc ->
                val d = doc.data ?: return@forEach
                val log = WorkoutSessionLog(workoutId = (d["workoutId"] as? Number)?.toInt() ?: 0, workoutName = d["workoutName"] as? String ?: "", date = (d["date"] as? Number)?.toLong() ?: 0L, exercisesDone = (d["exercisesDone"] as? Number)?.toInt() ?: 0, totalExercises = (d["totalExercises"] as? Number)?.toInt() ?: 0, durationSeconds = (d["durationSeconds"] as? Number)?.toLong() ?: 0L, caloriesBurned = (d["caloriesBurned"] as? Number)?.toInt() ?: 0, difficulty = d["difficulty"] as? String ?: "Intermediate", weightKg = (d["weightKg"] as? Number)?.toDouble() ?: 0.0)
                workoutDao.insertSessionLog(log)
            }
            val records = db.collection("users").document(uid).collection("exercise_records").get().await()
            records.documents.forEach { doc ->
                val d = doc.data ?: return@forEach
                val timestamp = d["date"] as? Timestamp
                val date = timestamp?.toDate() ?: java.util.Date()
                val log = WorkoutLog(workoutId = (d["workoutId"] as? Number)?.toInt() ?: 0, date = date, reps = d["reps"] as? String ?: "", weightKg = (d["weightKg"] as? Number)?.toDouble() ?: 0.0)
                workoutLogDao.insertLog(log)
            }
        } catch (e: Exception) { }
    }

    suspend fun deleteAllHistory() {
        workoutLogDao.deleteAll()
        workoutDao.deleteAllSessionLogs()
        workoutDao.deleteAllCustomWorkouts()
    }

    private data class RootJson(val exercises: List<Exercise>, val workouts: List<WorkoutJson>)
    private data class WorkoutJson(val id: Int, val name: String, val description: String, val category: String, val targetMuscle: String, val imageName: String, val difficulty: String, val duration: String, val exercises: List<WorkoutExerciseJson>)
    private data class WorkoutExerciseJson(val exerciseId: String, val category: String?, val sets: Int, val reps: String, val rest: String, val duration: String?)

    fun getLatestSessionLog(): Flow<WorkoutSessionLog?> = workoutDao.getLatestSessionLog()
    fun getAllSessionLogs(): Flow<List<WorkoutSessionLog>> = workoutDao.getAllSessionLogs()
    fun getWorkoutLogs(): Flow<List<WorkoutLog>> = workoutLogDao.getWorkoutLogs()
    fun getAllExercises(): Flow<List<Exercise>> = workoutDao.getAllExercises().map { list -> list.map { it.copy().apply { imageResId = application.resources.getIdentifier(imageName, "drawable", application.packageName) } } }
    fun getUniqueTargetMuscles(): Flow<List<String>> = workoutDao.getAllExercises().map { exercises -> exercises.flatMap { it.targetMuscle.split(',') }.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted() }

    private fun WorkoutWithExercises.applyImages(): WorkoutWithExercises {
        this.exerciseAssignments.forEach { assignment -> assignment.exercise.imageResId = application.resources.getIdentifier(assignment.exercise.imageName, "drawable", application.packageName) }
        return this
    }
}
