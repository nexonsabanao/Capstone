package com.example.nutriority.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nutriority.models.Article
import com.example.nutriority.models.Exercise
import com.example.nutriority.models.Meal
import com.example.nutriority.models.Workout
import com.example.nutriority.data.dao.ArticlesDao
import com.example.nutriority.data.dao.MealDao
import com.example.nutriority.data.dao.WorkoutDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader

@Database(
    entities = [Meal::class, Workout::class, Article::class, Exercise::class],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun articlesDao(): ArticlesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Empty for now
            }
        }

        fun getDatabase(context: Context, appScope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriority_database"
                )
                    .addMigrations(MIGRATION_6_7)
                    .addCallback(AppDatabaseCallback(context, appScope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val context: Context,
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        if (database.workoutDao().getWorkoutCount() == 0) { // Check workout table instead
                            database.withTransaction {
                                prePopulateDatabase(context, database)
                            }
                        }
                    }
                }
            }

            private suspend fun prePopulateDatabase(context: Context, db: AppDatabase) {
                val gson = Gson()
                val packageName = context.packageName

                // --- 1. Pre-populate Articles (No change) ---
                val articleType = object : TypeToken<List<Article>>() {}.type
                val articles: List<Article> = gson.fromJson(
                    context.assets.open("articles.json").bufferedReader().use(BufferedReader::readText),
                    articleType
                )
                articles.forEach { it.imageResId = context.resources.getIdentifier(it.imageName, "drawable", packageName) }
                db.articlesDao().insertAllArticles(articles)


                // --- 2. Pre-populate Meals (No change) ---
                val mealType = object : TypeToken<List<Meal>>() {}.type
                val meals: List<Meal> = gson.fromJson(
                    context.assets.open("meals.json").bufferedReader().use(BufferedReader::readText),
                    mealType
                )
                meals.forEach { it.imageResId = context.resources.getIdentifier(it.imageName, "drawable", packageName) }
                db.mealDao().insertAllMeals(meals)


                // --- 3. Pre-populate Workouts and Exercises (IMPROVED LOGIC) ---
                data class WorkoutJson(val workout: Workout, val exercises: List<Exercise>)
                val workoutType = object : TypeToken<List<WorkoutJson>>() {}.type
                val workoutData: List<WorkoutJson> = gson.fromJson(
                    context.assets.open("workouts.json").bufferedReader().use(BufferedReader::readText),
                    workoutType
                )

                workoutData.forEach { workoutJsonItem ->
                    // First, insert the parent workout to get its auto-generated ID
                    val workoutId = db.workoutDao().insertWorkout(workoutJsonItem.workout)

                    // Now, assign this new ID to all child exercises
                    workoutJsonItem.exercises.forEach { exercise ->
                        exercise.imageResId = context.resources.getIdentifier(exercise.imageName, "drawable", packageName)
                        // FIX: Corrected the property name from "workoutId" to "workoutId"
                        exercise.workoutId = workoutId.toInt()
                    }

                    // Finally, insert the correctly linked exercises
                    db.workoutDao().insertAllExercises(workoutJsonItem.exercises)
                }
            }
        }
    }
}
