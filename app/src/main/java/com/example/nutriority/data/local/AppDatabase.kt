package com.example.nutriority.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nutriority.R
import com.example.nutriority.data.model.Article
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader

@Database(
    entities = [Meal::class, Workout::class, Article::class, Exercise::class, WorkoutLog::class],
    version = 25,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun articlesDao(): ArticlesDao
    abstract fun workoutLogDao(): WorkoutLogDao

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
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(context.applicationContext, appScope))
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
                        if (database.workoutDao().getWorkoutCount() == 0) {
                            database.withTransaction {
                                prePopulateDatabase(context, database)
                            }
                        }
                    }
                }
            }

            @SuppressLint("DiscouragedApi")
            private suspend fun prePopulateDatabase(context: Context, db: AppDatabase) {
                val gson = Gson()
                try {
                    val packageName = context.packageName

                    fun getSafeImageResId(imageName: String?): Int {
                        if (imageName.isNullOrEmpty()) return R.drawable.img_balanced_diet
                        val resId = context.resources.getIdentifier(imageName, "drawable", packageName)
                        return if (resId != 0) resId else R.drawable.img_balanced_diet
                    }

                    // Pre-populate Articles
                    val articleType = object : TypeToken<List<Article>>() {}.type
                    val articlesJson = context.assets.open("articles.json").bufferedReader().use(BufferedReader::readText)
                    val articles: List<Article> = gson.fromJson(articlesJson, articleType)
                    articles.forEach { it.imageResId = getSafeImageResId(it.imageName) }
                    db.articlesDao().insertAllArticles(articles)

                    // Pre-populate Meals
                    val mealType = object : TypeToken<List<Meal>>() {}.type
                    val mealsJson = context.assets.open("meals.json").bufferedReader().use(BufferedReader::readText)
                    val meals: List<Meal> = gson.fromJson(mealsJson, mealType)
                    meals.forEach { it.imageResId = getSafeImageResId(it.imageName) }
                    db.mealDao().insertAllMeals(meals)

                    // Pre-populate Workouts
                    data class SimpleExercise(val name: String, val duration: String)
                    data class WorkoutJson(val workout: Workout, val exercises: List<Exercise>, val warmup: List<SimpleExercise>, val cooldown: List<SimpleExercise>)

                    val workoutType = object : TypeToken<List<WorkoutJson>>() {}.type
                    val workoutJson = context.assets.open("workouts.json").bufferedReader().use(BufferedReader::readText)
                    val workoutData: List<WorkoutJson> = gson.fromJson(workoutJson, workoutType)

                    workoutData.forEach { workoutJsonItem ->
                        val met = when (workoutJsonItem.workout.category.lowercase()) {
                            "cardio", "hiit" -> 8.0
                            "strength", "core" -> 5.0
                            "recovery", "flexibility" -> 2.5
                            else -> 5.0
                        }
                        
                        // Ensure all fields have values, even if missing in JSON
                        val workoutToInsert = workoutJsonItem.workout.copy(
                            metValue = met,
                            imageName = workoutJsonItem.workout.imageName ?: "img_balanced_diet"
                        )
                        val workoutId = db.workoutDao().insertWorkout(workoutToInsert)

                        workoutJsonItem.exercises.forEachIndexed { index, exercise ->
                            exercise.imageResId = getSafeImageResId(exercise.imageName)
                            exercise.workoutId = workoutId.toInt()
                            exercise.order = index
                            exercise.category = "Exercise"
                            // Fallback for missing difficulty in individual exercises
                            if (exercise.difficulty.isEmpty()) {
                                exercise.difficulty = workoutToInsert.difficulty
                            }
                            db.workoutDao().insertExercise(exercise)
                        }

                        workoutJsonItem.warmup.forEach { simpleExercise ->
                            val exercise = Exercise(
                                name = simpleExercise.name,
                                duration = simpleExercise.duration,
                                category = "Warm-up",
                                workoutId = workoutId.toInt(),
                                difficulty = workoutToInsert.difficulty
                            )
                            db.workoutDao().insertExercise(exercise)
                        }

                        workoutJsonItem.cooldown.forEach { simpleExercise ->
                            val exercise = Exercise(
                                name = simpleExercise.name,
                                duration = simpleExercise.duration,
                                category = "Cool-down",
                                workoutId = workoutId.toInt(),
                                difficulty = workoutToInsert.difficulty
                            )
                            db.workoutDao().insertExercise(exercise)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Failed to pre-populate database: ", e)
                }
            }
        }
    }
}
