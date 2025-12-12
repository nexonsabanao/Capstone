package com.example.nutriority.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Article
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.repository.ArticleRepository
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application, // Keep Application for resource access
    mealRepository: MealRepository,
    workoutRepository: WorkoutRepository,
    articleRepository: ArticleRepository
) : ViewModel() {

    val allMeals: StateFlow<List<Meal>>
    val allWorkouts: StateFlow<List<Workout>>
    val allArticles: StateFlow<List<Article>>

    init {
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
