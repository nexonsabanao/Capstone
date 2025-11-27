package com.example.nutriority.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.nutriority.data.dao.UserDao

@Database(entities = [User::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // Register converters at the database level
abstract class UserDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        // @Volatile ensures that the INSTANCE variable is always up-to-date and
        // visible to all other threads, preventing conflicts.
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getDatabase(context: Context): UserDatabase {
            // Return the existing instance if it's not null.
            // If it is null, create the database in a thread-safe way (synchronized).
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database" // The name of the database file on the device
                )
                    // This is a simple migration strategy. For a real app, you'd implement a proper Migration.
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
