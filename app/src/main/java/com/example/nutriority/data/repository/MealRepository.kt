package com.example.nutriority.data.repository

import com.example.nutriority.models.Meal
import com.example.nutriority.data.dao.MealDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Meal data.
 * It abstracts the data source (the MealDao) from the rest of the app.
 * This class is the single source of truth for all meal-related data.
 */
class MealRepository(private val mealDao: MealDao) {

    /**
     * A Flow that emits a list of all meals from the database, ordered by name.
     * The UI can collect this Flow to reactively update when the data changes.
     */
    val allMeals: Flow<List<Meal>> = mealDao.getAllMeals()

    /**
     * Retrieves a single meal by its ID.
     * @param mealId The ID of the meal to fetch.
     * @return A Flow that emits the specific Meal object, or null if not found.
     */
    fun getMealById(mealId: Int): Flow<Meal?> {
        return mealDao.getMealById(mealId)
    }

    /**
     * Inserts a new meal into the database.
     * This is a suspend function and must be called from a coroutine scope.
     * @param meal The meal object to insert.
     */
    suspend fun insert(meal: Meal) {
        mealDao.insertMeal(meal)
    }

    /**
     * Updates an existing meal in the database.
     * This is a suspend function and must be called from a coroutine scope.
     * @param meal The meal object to update.
     */
    suspend fun update(meal: Meal) {
        mealDao.updateMeal(meal)
    }

    /**
     * Deletes a specific meal from the database.
     * This is a suspend function and must be called from a coroutine scope.
     * @param meal The meal object to delete.
     */
    suspend fun delete(meal: Meal) {
        mealDao.deleteMeal(meal)
    }

    /**
     * Deletes all meals from the database.
     * This is a suspend function and must be called from a coroutine scope.
     */
    suspend fun deleteAll() {
        mealDao.deleteAllMeals()
    }
}
