package com.example.nutriority.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nutriority.data.model.User

@Dao
interface UserDao {
    /**
     * Inserts a user profile. If a profile already exists, it will be replaced.
     * This ensures you only ever have one user profile saved.
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertUser(user: User)

    /**
     * Retrieves the single user profile from the table for continuous observation.
     * LiveData will automatically update the UI when the data changes.
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUser(): LiveData<User>

    /**
     * Retrieves the single user profile from the table just once.
     * This is a suspend function to be called from a coroutine for the initial data fetch.
     * It is nullable to handle cases where the database is empty.
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserById(): User?

    /**
     * Deletes all data from the user_profile table. Useful for a logout or reset feature.
     */
    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}