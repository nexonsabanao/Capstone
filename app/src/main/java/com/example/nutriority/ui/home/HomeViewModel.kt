package com.example.nutriority.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Article
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.repository.ArticleRepository
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.planner.NutritionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    mealRepository: MealRepository,
    workoutRepository: WorkoutRepository,
    articleRepository: ArticleRepository,
    userRepository: UserRepository
) : ViewModel() {

    val allMeals: StateFlow<List<Meal>>
    val allWorkouts: StateFlow<List<Workout>>
    val allArticles: StateFlow<List<Article>>
    val isDataReady: StateFlow<Boolean>
    val calorieGoal: StateFlow<String>

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

        val recommendedWorkouts = combine(workoutRepository.allWorkouts, userRepository.getUser.asFlow()) { workouts, user ->
            val desiredDifficulties = when (user?.activityLevel) {
                "Sedentary" -> listOf("Beginner")
                "Lightly active" -> listOf("Beginner", "Intermediate")
                else -> listOf("Intermediate", "Advanced")
            }

            val filteredWorkouts = workouts.filter { it.difficulty in desiredDifficulties }

            if (filteredWorkouts.isEmpty()) {
                emptyList()
            } else {
                filteredWorkouts.groupBy { it.targetMuscle }.map { it.value.random() }.map {
                    it.apply {
                        imageResId = resources.getIdentifier(it.imageName, "drawable", packageName)
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allWorkouts = recommendedWorkouts

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

        isDataReady = combine(allMeals, allWorkouts) { meals, workouts ->
            meals.isNotEmpty() && workouts.isNotEmpty()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

        calorieGoal = userRepository.getUser.asFlow().map { user ->
            if (user != null) {
                NutritionCalculator.getCalorieRangeForDisplay(
                    user.weightKg,
                    user.heightCm,
                    user.age ?: 30,
                    user.gender,
                    user.activityLevel,
                    user.goal
                )
            } else {
                "1800-2200 kcal / day" // A sensible default
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
    }
}
