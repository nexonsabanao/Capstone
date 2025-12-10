package com.example.nutriority.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nutriority.R
import com.example.nutriority.data.local.Converters
import com.example.nutriority.data.local.ArticlesDao
import com.example.nutriority.data.local.MealDao
import com.example.nutriority.data.local.WorkoutDao
import com.example.nutriority.data.model.Article
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.model.Workout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader

@Database(
    entities = [Meal::class, Workout::class, Article::class, Exercise::class],
    version = 9, // Incremented version to trigger an update
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun articlesDao(): ArticlesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, appScope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriority_database"
                )
                    // This will destroy and re-create the database on a version change,
                    // which is useful during development.
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(context, appScope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val context: Context,
            private val scope: CoroutineScope
        ) : Callback() {

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
                try {
                val packageName = context.packageName

                // A safe way to get a resource ID or a default
                fun getSafeImageResId(imageName: String): Int {
                    val resId = context.resources.getIdentifier(imageName, "drawable", packageName)
                    return if (resId != 0) {
                        resId
                    } else {
                        Log.w("AppDatabase", "Missing drawable resource: $imageName. Using placeholder.")
                        R.drawable.img_balanced_diet // Use your placeholder image
                    }
                }

                // --- 1. Pre-populate Articles (Now safe) ---
                val articleType = object : TypeToken<List<Article>>() {}.type
                val articles: List<Article> = gson.fromJson(
                    context.assets.open("articles.json").bufferedReader().use(BufferedReader::readText),
                    articleType
                )
                articles.forEach { it.imageResId = getSafeImageResId(it.imageName) }
                db.articlesDao().insertAllArticles(articles)


                // --- 2. Pre-populate Meals (Now safe) ---
                val mealType = object : TypeToken<List<Meal>>() {}.type
                val meals: List<Meal> = gson.fromJson(
                    context.assets.open("meals.json").bufferedReader().use(BufferedReader::readText),
                    mealType
                )
                meals.forEach { it.imageResId = getSafeImageResId(it.imageName) }
                db.mealDao().insertAllMeals(meals)


                data class WorkoutJson(val workout: Workout, val exercises: List<Exercise>)
                val workoutType = object : TypeToken<List<WorkoutJson>>() {}.type
                val workoutData: List<WorkoutJson> = gson.fromJson(
                    context.assets.open("workouts.json").bufferedReader().use(BufferedReader::readText),
                    workoutType
                )

                workoutData.forEach { workoutJsonItem ->
                    val workoutId = db.workoutDao().insertWorkout(workoutJsonItem.workout)

                    workoutJsonItem.exercises.forEach { exercise ->
                        exercise.imageResId = getSafeImageResId(exercise.imageName)
                        exercise.workoutId = workoutId.toInt()
                    }

                    db.workoutDao().insertAllExercises(workoutJsonItem.exercises)
                }
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Failed to pre-populate database: ", e)
                }
            }
        }
    }
}