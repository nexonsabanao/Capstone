package com.example.nutriority.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.nutriority.data.UserDatabase
import com.example.nutriority.data.local.AppDatabase
import com.example.nutriority.data.local.ArticlesDao
import com.example.nutriority.data.local.DailyMealLogDao
import com.example.nutriority.data.local.MealDao
import com.example.nutriority.data.local.UserDao
import com.example.nutriority.data.local.WorkoutDao
import com.example.nutriority.data.local.WorkoutLogDao
import com.example.nutriority.data.repository.ArticleRepository
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.RecommendedWorkoutRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.planner.MealPlanner
import com.example.nutriority.planner.WorkoutGenerator
import com.example.nutriority.planner.WorkoutPlanner
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }

    @Provides
    @Singleton
    fun provideUserDatabase(@ApplicationContext context: Context): UserDatabase {
        return Room.databaseBuilder(
            context,
            UserDatabase::class.java,
            "user_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideUserDao(database: UserDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideUserRepository(userDao: UserDao): UserRepository {
        return UserRepository(userDao)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(application: Application, scope: CoroutineScope): AppDatabase {
        return AppDatabase.getDatabase(application, scope)
    }

    @Provides
    fun provideMealDao(database: AppDatabase): MealDao {
        return database.mealDao()
    }

    @Provides
    fun provideDailyMealLogDao(database: AppDatabase): DailyMealLogDao {
        return database.dailyMealLogDao()
    }

    @Provides
    fun provideMealRepository(
        mealDao: MealDao, 
        dailyMealLogDao: DailyMealLogDao, 
        application: Application
    ): MealRepository {
        return MealRepository(mealDao, dailyMealLogDao, application)
    }

    @Provides
    fun provideWorkoutDao(database: AppDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideWorkoutLogDao(database: AppDatabase): WorkoutLogDao {
        return database.workoutLogDao()
    }

    @Provides
    fun provideWorkoutRepository(workoutDao: WorkoutDao, workoutLogDao: WorkoutLogDao, application: Application): WorkoutRepository {
        return WorkoutRepository(workoutDao, workoutLogDao, application)
    }

    @Provides
    fun provideArticleDao(database: AppDatabase): ArticlesDao {
        return database.articlesDao()
    }

    @Provides
    fun provideArticleRepository(articleDao: ArticlesDao, application: Application): ArticleRepository {
        return ArticleRepository(articleDao, application)
    }

    @Provides
    @Singleton
    fun provideMealPlanner(mealRepository: MealRepository): MealPlanner {
        return MealPlanner(mealRepository)
    }

    @Provides
    @Singleton
    fun provideWorkoutGenerator(): WorkoutGenerator {
        return WorkoutGenerator()
    }

    @Provides
    @Singleton
    fun provideRecommendedWorkoutRepository(
        workoutDao: WorkoutDao
    ): RecommendedWorkoutRepository {
        return RecommendedWorkoutRepository(workoutDao)
    }

    @Provides
    @Singleton
    fun provideWorkoutPlanner(
        workoutRepository: WorkoutRepository,
        workoutGenerator: WorkoutGenerator,
        application: Application
    ): WorkoutPlanner {
        return WorkoutPlanner(workoutRepository, workoutGenerator, application)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("MealPlanPrefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
