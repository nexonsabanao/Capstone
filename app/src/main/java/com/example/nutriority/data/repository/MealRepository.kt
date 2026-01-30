package com.example.nutriority.data.repository

import android.app.Application
import android.util.Log
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.local.MealDao
import com.example.nutriority.data.local.DailyMealLogDao
import com.example.nutriority.data.model.DailyMealLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.BufferedReader
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
                imageResId = resources.getIdentifier(it.imageName, "drawable", packageName)
            }
        }
    }

    suspend fun getAllMealsList(): List<Meal> {
        return allMeals.first()
    }

    fun getMealById(mealId: Int): Flow<Meal?> {
        return mealDao.getMealById(mealId)
    }

    suspend fun ensureLibraryIsLoaded() {
        if (mealDao.getAllMeals().first().isNotEmpty()) return
        
        try {
            val gson = Gson()
            val mealsJson = application.assets.open("meals.json").bufferedReader().use(BufferedReader::readText)
            val meals: List<Meal> = gson.fromJson(mealsJson, object : TypeToken<List<Meal>>() {}.type)
            mealDao.insertAllMeals(meals)
            Log.d("Restore", "Meal Library Loaded")
        } catch (e: Exception) {
            Log.e("Restore", "Failed to load meal library", e)
        }
    }

    // --- DAILY LOGGING ---

    suspend fun logMeal(meal: Meal) {
        val log = DailyMealLog(
            mealId = meal.id,
            name = meal.name,
            calories = meal.calories,
            protein = (meal.calories * 0.15 / 4).toInt(), 
            carbs = (meal.calories * 0.50 / 4).toInt(),
            fats = (meal.calories * 0.35 / 9).toInt(),
            time = meal.time ?: "Snack",
            date = System.currentTimeMillis(),
            imageName = meal.imageName
        )
        dailyMealLogDao.insertLog(log)
        
        // Sync to cloud
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("daily_meal_logs").add(log)
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
            db.collection("users").document(uid).collection("meal_logs").document(meal.id.toString()).set(meal)
        }
    }

    suspend fun update(meal: Meal) {
        mealDao.updateMeal(meal)
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("meal_logs").document(meal.id.toString()).set(meal)
        }
    }

    suspend fun delete(meal: Meal) {
        mealDao.deleteMeal(meal)
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("meal_logs").document(meal.id.toString()).delete()
        }
    }

    suspend fun restoreMealsFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val snapshot = db.collection("users").document(uid).collection("meal_logs").get().await()
            val meals = snapshot.toObjects(Meal::class.java)
            meals.forEach { mealDao.insertMeal(it) }
        } catch (e: Exception) { }
    }

    suspend fun deleteAll() {
        mealDao.deleteAllMeals()
    }
}
