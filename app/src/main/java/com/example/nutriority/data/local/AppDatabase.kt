package com.example.nutriority.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.room.AutoMigration
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader

@Database(
    entities = [Meal::class, Workout::class, Article::class, Exercise::class],
    version = 20, // Incremented version for the new schema
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
                    .fallbackToDestructiveMigration(dropAllTables = true)
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
                        // Check if the database is empty before populating
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

                    fun getSafeImageResId(imageName: String): Int {
                        val resId = context.resources.getIdentifier(imageName, "drawable", packageName)
                        return if (resId != 0) resId else R.drawable.img_balanced_diet
                    }

                    // Pre-populate Articles
                    val articleType = object : TypeToken<List<Article>>() {}.type
                    val articles: List<Article> = gson.fromJson(context.assets.open("articles.json").bufferedReader().use(BufferedReader::readText), articleType)
                    articles.forEach { it.imageResId = getSafeImageResId(it.imageName) }
                    db.articlesDao().insertAllArticles(articles)

                    // Pre-populate Meals
                    val mealType = object : TypeToken<List<Meal>>() {}.type
                    val meals: List<Meal> = gson.fromJson(context.assets.open("meals.json").bufferedReader().use(BufferedReader::readText), mealType)
                    meals.forEach { it.imageResId = getSafeImageResId(it.imageName) }
                    db.mealDao().insertAllMeals(meals)

                    // Define structure for workouts.json
                    data class SimpleExercise(val name: String, val duration: String)
                    data class WorkoutJson(val workout: Workout, val exercises: List<Exercise>, val warmup: List<SimpleExercise>, val cooldown: List<SimpleExercise>)
                    
                    val workoutType = object : TypeToken<List<WorkoutJson>>() {}.type
                    val workoutData: List<WorkoutJson> = gson.fromJson(context.assets.open("workouts.json").bufferedReader().use(BufferedReader::readText), workoutType)

                    workoutData.forEach { workoutJsonItem ->
                        val workoutId = db.workoutDao().insertWorkout(workoutJsonItem.workout)

                        // Insert main exercises
                        workoutJsonItem.exercises.forEachIndexed { index, exercise ->
                            exercise.imageResId = getSafeImageResId(exercise.imageName)
                            exercise.workoutId = workoutId.toInt()
                            exercise.order = index
                            exercise.category = "Exercise"
                            db.workoutDao().insertExercise(exercise)
                        }

                        // Insert warm-up exercises
                        workoutJsonItem.warmup.forEach { simpleExercise ->
                            val exercise = Exercise(
                                name = simpleExercise.name,
                                duration = simpleExercise.duration,
                                category = "Warm-up",
                                workoutId = workoutId.toInt()
                            )
                            db.workoutDao().insertExercise(exercise)
                        }
                        
                        // Insert cool-down exercises
                        workoutJsonItem.cooldown.forEach { simpleExercise ->
                            val exercise = Exercise(
                                name = simpleExercise.name,
                                duration = simpleExercise.duration,
                                category = "Cool-down",
                                workoutId = workoutId.toInt()
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