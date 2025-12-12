package com.example.nutriority.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.nutriority.data.local.Converters
import com.example.nutriority.data.local.UserDao
import com.example.nutriority.data.model.User

@Database(entities = [User::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class) // Register converters at the database level
abstract class UserDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

}
