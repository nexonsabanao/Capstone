package com.example.nutriority.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.nutriority.data.UserDatabase
import com.example.nutriority.data.local.AppDatabase
import com.example.nutriority.data.local.ArticlesDao
import com.example.nutriority.data.local.MealDao
import com.example.nutriority.data.local.UserDao
import com.example.nutriority.data.local.WorkoutDao
import com.example.nutriority.data.repository.ArticleRepository
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.planner.MealPlanner
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
    fun provideMealRepository(mealDao: MealDao): MealRepository {
        return MealRepository(mealDao)
    }

    @Provides
    fun provideWorkoutDao(database: AppDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideWorkoutRepository(workoutDao: WorkoutDao): WorkoutRepository {
        return WorkoutRepository(workoutDao)
    }

    @Provides
    fun provideArticleDao(database: AppDatabase): ArticlesDao {
        return database.articlesDao()
    }

    @Provides
    fun provideArticleRepository(articleDao: ArticlesDao): ArticleRepository {
        return ArticleRepository(articleDao)
    }

    @Provides
    @Singleton
    fun provideMealPlanner(mealRepository: MealRepository): MealPlanner {
        return MealPlanner(mealRepository)
    }

    @Provides
    @Singleton
    fun provideWorkoutPlanner(workoutRepository: WorkoutRepository): WorkoutPlanner {
        return WorkoutPlanner(workoutRepository)
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
