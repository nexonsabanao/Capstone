package com.example.nutriority.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.nutriority.data.local.Converters
import com.example.nutriority.data.local.UserDao
import com.example.nutriority.data.model.User

@Database(entities = [User::class], version = 3, exportSchema = false)
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
                val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
                    // Use the same parameter name as the supertype (db) to avoid a Kotlin warning
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE user_profile ADD COLUMN age INTEGER")
                    }
                }
                val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
                    // Keep parameter name in sync with Migration's signature (db)
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Remove workoutPreference column by recreating the table without it.
                        db.execSQL("CREATE TABLE user_profile_new (id INTEGER PRIMARY KEY NOT NULL, gender TEXT NOT NULL, age INTEGER, heightCm REAL NOT NULL, weightKg REAL NOT NULL, unitSystem TEXT NOT NULL, activityLevel TEXT NOT NULL, goal TEXT NOT NULL, preferredDiet TEXT NOT NULL, excludedIngredients TEXT)")
                        db.execSQL("INSERT INTO user_profile_new (id, gender, age, heightCm, weightKg, unitSystem, activityLevel, goal, preferredDiet, excludedIngredients) SELECT id, gender, age, heightCm, weightKg, unitSystem, activityLevel, goal, preferredDiet, excludedIngredients FROM user_profile")
                        db.execSQL("DROP TABLE user_profile")
                        db.execSQL("ALTER TABLE user_profile_new RENAME TO user_profile")
                    }
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
