package com.example.nutriority.data.repository

import android.util.Log
import com.example.nutriority.data.local.WorkoutDao
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutExercise
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendedWorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao
) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun syncOfficialWorkoutsFromCloud() {
        try {
            val snapshot = db.collection("workoutsOfficial").get().await()
            if (snapshot.isEmpty) return

            val allExercises = workoutDao.getAllExercises().first()

            snapshot.documents.forEachIndexed { i, doc ->
                val workoutData = doc.get("workout") as? Map<String, Any> ?: return@forEachIndexed
                val workoutId = (workoutData["id"] as? Number)?.toInt() ?: (i + 1)
                
                val (officialWorkout, assignments) = parseWorkoutDocument(doc, workoutId, allExercises) ?: return@forEachIndexed
                
                workoutDao.updateWorkoutWithExercises(officialWorkout, assignments)
            }
            Log.d("OfficialWorkouts", "Synced official workouts.")
        } catch (e: Exception) {
            Log.e("OfficialWorkouts", "Error syncing: ${e.message}")
        }
    }

    /**
     * Fetches a specific workout's original structure for the Reset functionality.
     */
    suspend fun getOriginalAssignments(workoutId: Int): List<WorkoutExercise> {
        return try {
            val doc = db.collection("workoutsOfficial").document("workout_$workoutId").get().await()
            val allExercises = workoutDao.getAllExercises().first()
            val result = parseWorkoutDocument(doc, workoutId, allExercises)
            result?.second ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseWorkoutDocument(
        doc: com.google.firebase.firestore.DocumentSnapshot, 
        workoutId: Int, 
        allExercises: List<Exercise>
    ): Pair<Workout, List<WorkoutExercise>>? {
        val workoutData = doc.get("workout") as? Map<String, Any> ?: return null
        val warmupData = doc.get("warmup") as? List<Map<String, Any>> ?: emptyList()
        val exercisesData = doc.get("exercises") as? List<Map<String, Any>> ?: emptyList()
        val cooldownData = doc.get("cooldown") as? List<Map<String, Any>> ?: emptyList()

        val officialWorkout = Workout(
            id = workoutId,
            name = workoutData["name"] as? String ?: "",
            description = workoutData["description"] as? String ?: "",
            category = "Official",
            difficulty = workoutData["difficulty"] as? String ?: "Beginner",
            targetMuscle = workoutData["targetMuscle"] as? String ?: "",
            imageName = workoutData["imageName"] as? String ?: "img_balanced_diet",
            duration = workoutData["duration"] as? String ?: "30 min",
            tags = (workoutData["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            includeWarmupCooldown = true
        )

        val assignments = mutableListOf<WorkoutExercise>()
        var order = 0

        fun addGroup(data: List<Map<String, Any>>, category: String) {
            data.forEach { item ->
                val name = item["name"] as? String ?: ""
                findExerciseIdByName(name, allExercises)?.let { exId ->
                    assignments.add(WorkoutExercise(
                        workoutId = workoutId,
                        exerciseId = exId,
                        category = category,
                        sets = (item["sets"] as? Number)?.toInt() ?: (if (category == "Exercise") 3 else 1),
                        reps = item["reps"] as? String ?: "10",
                        rest = item["rest"] as? String ?: "60s",
                        duration = item["duration"] as? String ?: (if (category == "Exercise") "" else "1 min"),
                        order = order++
                    ))
                }
            }
        }

        addGroup(warmupData, "warmup")
        addGroup(exercisesData, "Exercise")
        addGroup(cooldownData, "cooldown")

        return officialWorkout to assignments
    }

    private fun findExerciseIdByName(name: String, allExercises: List<Exercise>): String? {
        return allExercises.find { it.name.equals(name, true) }?.id 
            ?: allExercises.find { it.name.contains(name, true) }?.id
    }

    suspend fun seedOfficialWorkouts(ignored: Any) {
        syncOfficialWorkoutsFromCloud()
    }
}
