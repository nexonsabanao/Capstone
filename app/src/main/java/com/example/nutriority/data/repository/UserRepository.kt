package com.example.nutriority.data.repository

import com.example.nutriority.data.model.User
import com.example.nutriority.data.local.UserDao
import com.example.nutriority.data.local.WorkoutDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val workoutDao: WorkoutDao,
    private val db: FirebaseFirestore
) {
    private val auth = FirebaseAuth.getInstance()

    val getUser: Flow<User?> = userDao.getUser()

    suspend fun getInitialUser(): User? {
        return userDao.getUserById()
    }

    suspend fun insertUser(user: User): Boolean {
        val localSuccess = userDao.insertUser(user) > 0
        
        auth.currentUser?.uid?.let { uid ->
            // First, fetch current cloud status to avoid overwriting a "deleted" status
            val currentDoc = try { 
                db.collection("users").document(uid).get().await() 
            } catch (e: Exception) { null }
            
            val cloudStatus = currentDoc?.getString("status") ?: user.status

            val userMap = hashMapOf(
                "id" to uid,
                "email" to user.email,
                "name" to user.name,
                "profileImageUrl" to user.profileImageUrl, 
                "gender" to user.gender,
                "birthDate" to user.birthDate,
                "heightCm" to user.heightCm,
                "weightKg" to user.weightKg,
                "unitSystem" to user.unitSystem,
                "activityLevel" to user.activityLevel,
                "goal" to user.goal,
                "preferredDiet" to user.preferredDiet,
                "excludedIngredients" to user.excludedIngredients,
                "personalizedPlanJson" to user.personalizedPlanJson,
                "mealPlanJson" to user.mealPlanJson,
                "lastCompletedWorkoutDay" to user.lastCompletedWorkoutDay,
                "status" to cloudStatus, // Preserve the "deleted" status if set by admin
                "totalCaloriesBurned" to user.totalCaloriesBurned,
                "totalWorkoutMinutes" to user.totalWorkoutMinutes,
                "totalWorkoutsCompleted" to user.totalWorkoutsCompleted
            )
            try {
                db.collection("users").document(uid).set(userMap).await()
            } catch (e: Exception) { }
        }
        return localSuccess
    }

    suspend fun restoreUserFromCloud(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val document = db.collection("users").document(uid).get().await()
            val data = document.data
            if (data != null) {
                val restoredUser = User(
                    id = 1,
                    email = data["email"] as? String ?: "",
                    name = data["name"] as? String ?: "",
                    profileImageUrl = data["profileImageUrl"] as? String ?: "", 
                    gender = data["gender"] as? String ?: "",
                    birthDate = (data["birthDate"] as? Number)?.toLong(),
                    heightCm = (data["heightCm"] as? Number)?.toDouble() ?: 0.0,
                    weightKg = (data["weightKg"] as? Number)?.toDouble() ?: 0.0,
                    unitSystem = data["unitSystem"] as? String ?: "METRIC",
                    activityLevel = data["activityLevel"] as? String ?: "",
                    goal = data["goal"] as? String ?: "",
                    preferredDiet = data["preferredDiet"] as? String ?: "",
                    excludedIngredients = (data["excludedIngredients"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    personalizedPlanJson = data["personalizedPlanJson"] as? String,
                    mealPlanJson = data["mealPlanJson"] as? String,
                    lastCompletedWorkoutDay = (data["lastCompletedWorkoutDay"] as? Number)?.toInt() ?: 0,
                    status = data["status"] as? String ?: "active",
                    totalCaloriesBurned = (data["totalCaloriesBurned"] as? Number)?.toInt() ?: 0,
                    totalWorkoutMinutes = (data["totalWorkoutMinutes"] as? Number)?.toLong() ?: 0L,
                    totalWorkoutsCompleted = (data["totalWorkoutsCompleted"] as? Number)?.toInt() ?: 0
                )
                userDao.insertUser(restoredUser)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun recalculateUserStats() {
        val user = userDao.getUserById() ?: return
        val sessionLogs = workoutDao.getAllSessionLogs().first()
        
        var totalCals = 0
        var totalSecs = 0L
        var completedCount = 0
        
        sessionLogs.forEach { log ->
            if (log.workoutId > 0 || log.totalExercises > 0) {
                totalCals += log.caloriesBurned
                totalSecs += log.durationSeconds
                completedCount++
            }
        }
        
        val updatedUser = user.copy(
            totalCaloriesBurned = totalCals,
            totalWorkoutMinutes = totalSecs / 60,
            totalWorkoutsCompleted = completedCount
        )
        userDao.insertUser(updatedUser)
    }

    suspend fun deleteAll() {
        userDao.deleteAll()
    }
}
