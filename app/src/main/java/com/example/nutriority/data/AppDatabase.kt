package com.example.nutriority.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nutriority.Models.Article
import com.example.nutriority.Models.Exercise
import com.example.nutriority.Models.Meal
import com.example.nutriority.Models.Workout
import com.example.nutriority.data.dao.ArticlesDao
import com.example.nutriority.data.dao.MealDao
import com.example.nutriority.data.dao.WorkoutDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader

@Database(
    entities = [Meal::class, Workout::class, Article::class, Exercise::class],
    version = 6, // 1. INCREMENT version due to schema changes
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriority_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        // Call all three pre-population functions
                        prePopulateWorkoutsAndExercises(context, database.workoutDao())
                        prePopulateMeals(context, database.mealDao())
                        prePopulateArticles(context, database.articlesDao())
                    }
                }
            }

            private suspend fun prePopulateArticles(context: Context, articlesDao: ArticlesDao) {
                val articles = listOf(
                    Article(
                        title = "The Benefits of a High-Protein Diet",
                        author = "Jane Doe, PhD",
                        readingTime = "5 min read",
                        category = "Nutrition",
                        imageResId = context.resources.getIdentifier("article_protein", "drawable", context.packageName)
                    ),
                    Article(
                        title = "Understanding Macronutrients",
                        author = "John Smith, R.D.",
                        readingTime = "8 min read",
                        category = "Nutrition",
                        imageResId = context.resources.getIdentifier("article_macros", "drawable", context.packageName)
                    ),
                    Article(
                        title = "Mindful Eating: A Beginner's Guide",
                        author = "Emily White",
                        readingTime = "6 min read",
                        category = "Wellness",
                        imageResId = context.resources.getIdentifier("article_mindful", "drawable", context.packageName)
                    )
                )

                // Insert all articles into the database
                articles.forEach { articlesDao.insertArticle(it) }
            }

            private suspend fun prePopulateMeals(context: Context, mealDao: MealDao) {
                val meals = listOf(
                    Meal(
                        name = "Avocado Toast",
                        calories = "350 kcal",
                        category = "Vegetarian",
                        time = "Breakfast",
                        ingredients = listOf("2 slices of whole-wheat bread", "1 ripe avocado", "1 pinch of salt", "1 pinch of red pepper flakes"),
                        imageResId = context.resources.getIdentifier("meal_avocado_toast", "drawable", context.packageName)
                    ),
                    Meal(
                        name = "Chicken Salad",
                        calories = "450 kcal",
                        category = "Low Carb",
                        time = "Lunch",
                        ingredients = listOf("150g grilled chicken breast", "50g mixed greens", "1/2 cucumber", "1/4 red onion", "2 tbsp vinaigrette"),
                        imageResId = context.resources.getIdentifier("meal_chicken_salad", "drawable", context.packageName)
                    ),
                    Meal(
                        name = "Quinoa Bowl",
                        calories = "400 kcal",
                        category = "Balanced Diet",
                        time = "Dinner",
                        ingredients = listOf("1 cup cooked quinoa", "1/2 cup black beans", "1/2 cup corn", "1/4 avocado", "Salsa and lime to taste"),
                        imageResId = context.resources.getIdentifier("meal_quinoa_bowl", "drawable", context.packageName)
                    )
                )
                meals.forEach { mealDao.insertMeal(it) }
            }

            private suspend fun prePopulateWorkoutsAndExercises(context: Context, workoutDao: WorkoutDao) {
                // 2. UPDATE Workout to include targetMuscle
                val calisthenicsWorkout = Workout(
                    id = 1,
                    name = "Calisthenics Basics",
                    description = "Master the fundamentals of bodyweight training.",
                    category = "Beginner",
                    targetMuscle = "Full Body", // Added targetMuscle
                    imageResId = context.resources.getIdentifier("workout_calisthenics", "drawable", context.packageName)
                )
                workoutDao.insertWorkout(calisthenicsWorkout)

                // 3. UPDATE Exercises to include targetMuscle
                val exercises = listOf(
                    Exercise(
                        workoutId = 1,
                        name = "Push-Ups",
                        description = "A classic bodyweight exercise that strengthens the chest, shoulders, and triceps.",
                        reps = "3 sets of 12-15 reps",
                        targetMuscle = "Chest, Shoulders, Triceps", // Added targetMuscle
                        imageResId = context.resources.getIdentifier("exercise_pushup", "drawable", context.packageName)
                    ),
                    Exercise(
                        workoutId = 1,
                        name = "Squats",
                        description = "A fundamental lower body exercise that targets the quadriceps, hamstrings, and glutes.",
                        reps = "3 sets of 15-20 reps",
                        targetMuscle = "Legs, Glutes", // Added targetMuscle
                        imageResId = context.resources.getIdentifier("exercise_squat", "drawable", context.packageName)
                    ),
                    Exercise(
                        workoutId = 1,
                        name = "Plank",
                        description = "An isometric core strength exercise.",
                        reps = "3 sets, hold for 30-60 seconds",
                        targetMuscle = "Core", // Added targetMuscle
                        imageResId = context.resources.getIdentifier("exercise_plank", "drawable", context.packageName)
                    )
                )
                exercises.forEach { workoutDao.insertExercise(it) }
            }
        }
    }
}
