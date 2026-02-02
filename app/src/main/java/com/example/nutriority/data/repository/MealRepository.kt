package com.example.nutriority.data.repository

import android.app.Application
import android.util.Log
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.local.MealDao
import com.example.nutriority.data.local.DailyMealLogDao
import com.example.nutriority.data.model.DailyMealLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class MealRepository(
    private val mealDao: MealDao, 
    private val dailyMealLogDao: DailyMealLogDao,
    private val application: Application
) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val allMeals: Flow<List<Meal>> = mealDao.getAllMeals().map { meals ->
        meals.map {
            it.apply {
                val resources = application.resources
                val packageName = application.packageName
                if (imageName.isNotEmpty() && !imageName.startsWith("http")) {
                    imageResId = resources.getIdentifier(imageName, "drawable", packageName)
                }
            }
        }
    }

    suspend fun getAllMealsList(): List<Meal> {
        return allMeals.first()
    }

    fun getMealById(mealId: String): Flow<Meal?> {
        return mealDao.getMealById(mealId)
    }

    /**
     * Synchronizes the global meal library from Firestore to the local database.
     */
    suspend fun syncMealsFromCloud() {
        try {
            val snapshot = db.collection("meals").get().await()
            val cloudMeals = snapshot.toObjects(Meal::class.java)
            if (cloudMeals.isNotEmpty()) {
                mealDao.insertAllMeals(cloudMeals)
                Log.d("MealRepo", "Synced ${cloudMeals.size} meals from Firestore")
            }
        } catch (e: Exception) {
            Log.e("MealRepo", "Error syncing meals: ${e.message}")
        }
    }

    suspend fun ensureLibraryIsLoaded() {
        // Only sync from cloud as per user request to stop using meals.json
        syncMealsFromCloud()
    }

    // --- DAILY LOGGING ---

    suspend fun logMeal(meal: Meal) {
        val log = DailyMealLog(
            mealId = meal.id,
            name = meal.name,
            calories = meal.calories,
            protein = meal.macros.protein, 
            carbs = meal.macros.carbs,
            fats = meal.macros.fats,
            mealTime = meal.mealTime ?: "Snack",
            date = System.currentTimeMillis(),
            imageName = meal.imageName
        )
        insertDailyLog(log)
    }

    suspend fun logManualMeal(
        name: String,
        protein: Int,
        carbs: Int,
        fats: Int,
        time: String,
        ingredients: List<String>,
        manualCalories: Int = 0
    ) {
        val finalCalories = if (manualCalories > 0) {
            manualCalories
        } else {
            (protein * 4) + (carbs * 4) + (fats * 9)
        }

        val log = DailyMealLog(
            mealId = "-1", 
            name = name,
            calories = finalCalories,
            protein = protein,
            carbs = carbs,
            fats = fats,
            mealTime = time,
            date = System.currentTimeMillis(),
            imageName = "bg_image_placeholder" 
        )
        insertDailyLog(log)
    }

    private suspend fun insertDailyLog(log: DailyMealLog) {
        dailyMealLogDao.insertLog(log)
        
        auth.currentUser?.uid?.let { uid ->
            try {
                db.collection("users").document(uid).collection("daily_meal_logs").add(log).await()
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync meal log", e)
            }
        }
    }

    fun getLogsForToday(): Flow<List<DailyMealLog>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val end = calendar.timeInMillis
        
        return dailyMealLogDao.getLogsForDay(start, end)
    }

    suspend fun deleteMealLog(logId: Int) {
        dailyMealLogDao.deleteLog(logId)
    }

    // --- CRUD ---

    suspend fun insert(meal: Meal) {
        mealDao.insertMeal(meal)
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("meal_logs").document(meal.id).set(meal)
        }
    }

    suspend fun update(meal: Meal) {
        mealDao.updateMeal(meal)
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("meal_logs").document(meal.id).set(meal)
        }
    }

    suspend fun delete(meal: Meal) {
        mealDao.deleteMeal(meal)
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("meal_logs").document(meal.id).delete()
        }
    }

    suspend fun restoreMealsFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        try {
            // Restore library/favorites
            val snapshot = db.collection("users").document(uid).collection("meal_logs").get().await()
            val meals = snapshot.toObjects(Meal::class.java)
            meals.forEach { mealDao.insertMeal(it) }
            
            // Restore daily logging history
            val logSnapshot = db.collection("users").document(uid).collection("daily_meal_logs").get().await()
            val logs = logSnapshot.toObjects(DailyMealLog::class.java)
            logs.forEach { dailyMealLogDao.insertLog(it) }
        } catch (e: Exception) { }
    }

    suspend fun deleteAll() {
        mealDao.deleteAllMeals()
        dailyMealLogDao.deleteAll()
    }
}