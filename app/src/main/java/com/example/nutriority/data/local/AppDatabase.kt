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
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.data.model.WorkoutSessionLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader

@Database(
    entities = [Meal::class, Workout::class, Article::class, Exercise::class, WorkoutLog::class, WorkoutExercise::class, WorkoutSessionLog::class],
    version = 32, // Incremented version
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

                    // Pre-populate New Workouts structure
                    data class WorkoutExerciseJson(val exerciseId: String, val category: String?, val sets: Int, val reps: String, val rest: String, val duration: String?)
                    data class WorkoutJson(val id: Int, val name: String, val description: String, val category: String, val targetMuscle: String, val imageName: String, val difficulty: String, val duration: String, val exercises: List<WorkoutExerciseJson>)
                    data class RootJson(val exercises: List<Exercise>, val workouts: List<WorkoutJson>)

                    val rootJsonStr = context.assets.open("workouts.json").bufferedReader().use(BufferedReader::readText)
                    val rootData: RootJson = gson.fromJson(rootJsonStr, RootJson::class.java)

                    rootData.exercises.forEach { exercise ->
                        exercise.imageResId = getSafeImageResId(exercise.imageName)
                        db.workoutDao().insertExercise(exercise)
                    }

                    rootData.workouts.forEach { wJson ->
                        val met = when (wJson.category.lowercase()) {
                            "cardio", "hiit" -> 8.0
                            "strength", "core" -> 5.0
                            "recovery", "flexibility" -> 2.5
                            else -> 5.0
                        }
                        
                        val workout = Workout(
                            id = wJson.id,
                            name = wJson.name,
                            description = wJson.description,
                            category = wJson.category,
                            targetMuscle = wJson.targetMuscle,
                            imageName = wJson.imageName,
                            difficulty = wJson.difficulty,
                            duration = wJson.duration,
                            metValue = met
                        )
                        db.workoutDao().insertWorkout(workout)

                        wJson.exercises.forEachIndexed { index, weJson ->
                            val workoutExercise = WorkoutExercise(
                                workoutId = wJson.id,
                                exerciseId = weJson.exerciseId,
                                category = weJson.category ?: "Exercise",
                                sets = weJson.sets,
                                reps = weJson.reps,
                                rest = weJson.rest,
                                duration = weJson.duration ?: "",
                                order = index
                            )
                            db.workoutDao().insertWorkoutExercise(workoutExercise)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Failed to pre-populate database: ", e)
                }
            }
        }
    }
}
