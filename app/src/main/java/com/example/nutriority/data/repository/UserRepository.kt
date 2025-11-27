package com.example.nutriority.data.repository

import androidx.lifecycle.LiveData
import com.example.nutriority.data.User
import com.example.nutriority.data.dao.UserDao

/**
 * The UserRepository provides a clean API for data access to the rest of the application.
 * It abstracts the data sources (in this case, only the Room DAO) from the ViewModels.
 */
class UserRepository(private val userDao: UserDao) {

    // Expose LiveData to observe the user data from the database.
    // This is for continuous observation.
    val getUser: LiveData<User> = userDao.getUser()

    /**
     * A suspend function to get the initial user data just once.
     * This will be called from the ViewModel's coroutine scope on init.
     * It calls the DAO's suspend function.
     */
    suspend fun getInitialUser(): User? {
        return userDao.getUserById()
    }

    /**
     * A suspend function to insert or update a user profile in the database.
     * This will be called from a ViewModel's coroutine scope.
     */
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    /**
     * A suspend function to delete all user data.
     */
    suspend fun deleteAll() {
        userDao.deleteAll()
    }
}