package com.example.nutriority.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.local.AppDatabase
import com.example.nutriority.data.model.Article
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.repository.ArticleRepository
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val mealRepository: MealRepository
    private val workoutRepository: WorkoutRepository
    private val articleRepository: ArticleRepository

    val allMeals: StateFlow<List<Meal>>
    val allWorkouts: StateFlow<List<Workout>>
    val allArticles: StateFlow<List<Article>>

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        val mealDao = database.mealDao()
        val workoutDao = database.workoutDao()
        val articlesDao = database.articlesDao()

        mealRepository = MealRepository(mealDao)
        workoutRepository = WorkoutRepository(workoutDao)
        articleRepository = ArticleRepository(articlesDao)

        val resources = application.resources
        val packageName = application.packageName

        allMeals = mealRepository.allMeals.map { meals ->
            meals.map {
                it.apply {
                    imageResId = resources.getIdentifier(it.imageName, "drawable", packageName)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allWorkouts = workoutRepository.allWorkouts.map { workouts ->
            workouts.map {
                it.apply {
                    imageResId = resources.getIdentifier(it.imageName, "drawable", packageName)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allArticles = articleRepository.allArticles.map { articles ->
            articles.map {
                it.apply {
                    imageResId = resources.getIdentifier(it.imageName, "drawable", packageName)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
}
