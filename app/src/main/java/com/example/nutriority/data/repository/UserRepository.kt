package com.example.nutriority.data.repository

import androidx.lifecycle.LiveData
import com.example.nutriority.data.model.User
import com.example.nutriority.data.local.UserDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.tasks.await

class UserRepository(private val userDao: UserDao) {

    private val db = FirebaseFirestore.getInstance().apply {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestoreSettings = settings
    }
    private val auth = FirebaseAuth.getInstance()

    val getUser: LiveData<User> = userDao.getUser()

    suspend fun getInitialUser(): User? {
        return userDao.getUserById()
    }

    suspend fun insertUser(user: User): Boolean {
        val localSuccess = userDao.insertUser(user) > 0
        
        auth.currentUser?.uid?.let { uid ->
            val userMap = hashMapOf(
                "id" to user.id,
                "gender" to user.gender,
                "age" to user.age,
                "heightCm" to user.heightCm,
                "weightKg" to user.weightKg,
                "unitSystem" to user.unitSystem,
                "activityLevel" to user.activityLevel,
                "goal" to user.goal,
                "preferredDiet" to user.preferredDiet,
                "excludedIngredients" to user.excludedIngredients,
                "personalizedPlanJson" to user.personalizedPlanJson,
                "lastCompletedWorkoutDay" to user.lastCompletedWorkoutDay
            )
            try {
                // ADDED .await() to ensure sync completes
                db.collection("users").document(uid).set(userMap).await()
            } catch (e: Exception) {
                // Silently handle offline or sync errors
            }
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
                    gender = data["gender"] as? String ?: "",
                    age = (data["age"] as? Number)?.toInt(),
                    heightCm = (data["heightCm"] as? Number)?.toDouble() ?: 0.0,
                    weightKg = (data["weightKg"] as? Number)?.toDouble() ?: 0.0,
                    unitSystem = data["unitSystem"] as? String ?: "METRIC",
                    activityLevel = data["activityLevel"] as? String ?: "",
                    goal = data["goal"] as? String ?: "",
                    preferredDiet = data["preferredDiet"] as? String ?: "",
                    excludedIngredients = (data["excludedIngredients"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    personalizedPlanJson = data["personalizedPlanJson"] as? String,
                    lastCompletedWorkoutDay = (data["lastCompletedWorkoutDay"] as? Number)?.toInt() ?: 0
                )
                userDao.insertUser(restoredUser)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteAll() {
        userDao.deleteAll()
    }
}
