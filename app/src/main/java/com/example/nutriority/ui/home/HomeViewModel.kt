package com.example.nutriority.ui.home

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
        allMeals = mealRepository.allMeals.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allArticles = articleRepository.allArticles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        val recommendedWorkouts = combine(workoutRepository.allWorkouts, userRepository.getUser.asFlow()) { workouts, user ->
            val desiredDifficulties = when (user.activityLevel) {
                "Sedentary" -> listOf("Beginner")
                "Lightly active" -> listOf("Beginner", "Intermediate")
                else -> listOf("Intermediate", "Advanced")
            }

            val filteredWorkouts = workouts.filter { it.difficulty in desiredDifficulties }

            if (filteredWorkouts.isEmpty()) {
                emptyList()
            } else {
                filteredWorkouts.groupBy { it.targetMuscle }.map { it.value.random() }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allWorkouts = recommendedWorkouts

        isDataReady = combine(allMeals, allWorkouts) { meals, workouts ->
            meals.isNotEmpty() && workouts.isNotEmpty()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

        calorieGoal = userRepository.getUser.asFlow().map { user ->
            NutritionCalculator.getCalorieRangeForDisplay(
                user.weightKg,
                user.heightCm,
                user.age ?: 30,
                user.gender,
                user.activityLevel,
                user.goal
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "1800-2200 kcal / day"
        )
    }
}
