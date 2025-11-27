package com.example.nutriority.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.Models.Article
import com.example.nutriority.Models.Meal
import com.example.nutriority.Models.Workout
import com.example.nutriority.data.AppDatabase
import com.example.nutriority.data.repository.ArticleRepository
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Home screen.
 * It provides data to the UI and survives configuration changes.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // Create private repository instances
    private val mealRepository: MealRepository
    private val workoutRepository: WorkoutRepository
    private val articleRepository: ArticleRepository

    // Expose StateFlows from the repositories to the UI
    val allMeals: StateFlow<List<Meal>>
    val allWorkouts: StateFlow<List<Workout>>
    val allArticles: StateFlow<List<Article>>

    init {
        // Get DAOs from the AppDatabase instance
        val database = AppDatabase.getDatabase(application)
        val mealDao = database.mealDao()
        val workoutDao = database.workoutDao()
        val articlesDao = database.articlesDao()

        // Initialize repositories
        mealRepository = MealRepository(mealDao)
        workoutRepository = WorkoutRepository(workoutDao)
        articleRepository = ArticleRepository(articlesDao)

        // Convert the repository Flows into StateFlows for the UI to collect
        allMeals = mealRepository.allMeals.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Provide an empty list as the initial value
        )

        allWorkouts = workoutRepository.allWorkouts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Provide an empty list as the initial value
        )

        allArticles = articleRepository.allArticles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Provide an empty list as the initial value
        )
    }
}
