package com.example.nutriority.data.repository

import android.app.Application
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.local.MealDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MealRepository(private val mealDao: MealDao, private val application: Application) {

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

    suspend fun insert(meal: Meal) {
        mealDao.insertMeal(meal)
    }

    suspend fun update(meal: Meal) {
        mealDao.updateMeal(meal)
    }

    suspend fun delete(meal: Meal) {
        mealDao.deleteMeal(meal)
    }

    suspend fun deleteAll() {
        mealDao.deleteAllMeals()
    }
}
