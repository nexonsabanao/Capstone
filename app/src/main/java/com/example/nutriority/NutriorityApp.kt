package com.example.nutriority

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.RecommendedWorkoutRepository
import com.example.nutriority.data.repository.WorkoutRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NutriorityApp : Application() {

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var mealRepository: MealRepository
    @Inject lateinit var recommendedWorkoutRepository: RecommendedWorkoutRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        // Force Light Mode globally to prevent flickering in activities
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        
        // BACKGROUND OPTIMIZATION: Start syncing data silently in the background
        applicationScope.launch {
            try {
                workoutRepository.syncExercisesFromCloud()
                mealRepository.syncMealsFromCloud()
                recommendedWorkoutRepository.syncOfficialWorkoutsFromCloud()
            } catch (_: Exception) { }
        }
    }
}
