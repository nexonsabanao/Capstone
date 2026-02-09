package com.example.nutriority

import android.app.Application
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
        
        // BACKGROUND OPTIMIZATION: Start syncing data silently in the background
        // so the SplashFragment doesn't have to wait for it.
        applicationScope.launch {
            try {
                workoutRepository.syncExercisesFromCloud()
                mealRepository.syncMealsFromCloud()
                recommendedWorkoutRepository.syncOfficialWorkoutsFromCloud()
            } catch (e: Exception) { }
        }
    }
}
