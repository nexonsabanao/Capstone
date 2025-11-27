package com.example.nutriority.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nutriority.Models.Meal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    /**
     * Inserts a new meal into the table. If a meal with the same primary key
     * already exists, it will be replaced.
     * @param meal The meal object to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal)

    /**
     * Updates an existing meal in the table.
     * @param meal The meal object to update.
     */
    @Update
    suspend fun updateMeal(meal: Meal)

    /**
     * Deletes a specific meal from the table.
     * @param meal The meal object to delete.
     */
    @Delete
    suspend fun deleteMeal(meal: Meal)

    /**
     * Deletes all meals from the 'meals' table.
     */
    @Query("DELETE FROM meals")
    suspend fun deleteAllMeals()

    /**
     * Retrieves a specific meal by its ID. Returns a Flow for reactive updates.
     * @param mealId The ID of the meal to retrieve.
     * @return A Flow emitting the Meal object, or null if not found.
     */
    @Query("SELECT * FROM meals WHERE id = :mealId")
    fun getMealById(mealId: Int): Flow<Meal?>

    /**
     * Retrieves all meals from the table, ordered by name.
     * Using Flow is the modern recommended practice for reactive data streams from Room.
     * @return A Flow emitting a list of all Meal objects.
     */
    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMeals(): Flow<List<Meal>>
}
