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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val allMeals: StateFlow<List<Meal>>
    val allWorkouts: StateFlow<List<Workout>> // This remains the recommended list for Home
    val unfilteredWorkouts: StateFlow<List<Workout>> // Raw list for tabs to filter
    val allArticles: StateFlow<List<Article>>
    val isDataReady: StateFlow<Boolean>
    val calorieGoal: StateFlow<String>

    init {
        // Automatically sync data from Firestore when Home is opened
        viewModelScope.launch {
            articleRepository.syncArticlesFromCloud()
            mealRepository.syncMealsFromCloud()
            workoutRepository.syncExercisesFromCloud()
        }

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

        unfilteredWorkouts = workoutRepository.allWorkouts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        val recommendedWorkouts = combine(workoutRepository.allWorkouts, userRepository.getUser.asFlow()) { workouts, user ->
            if (user == null) return@combine emptyList()
            
            val desiredDifficulties = when (user.activityLevel) {
                "Sedentary", "Lightly active" -> listOf("Beginner")
                "Active" -> listOf("Beginner", "Intermediate")
                else -> listOf("Intermediate", "Advanced")
            }

            // Removed the id <= 25 restriction since workouts are now dynamic or cloud-based
            val filteredWorkouts = workouts.filter { it.difficulty in desiredDifficulties }

            if (filteredWorkouts.isEmpty()) {
                // If no difficulty match, just show some random ones so the screen isn't empty
                workouts.shuffled().take(5)
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
            if (user == null) return@map "1800-2200 kcal / day"
            
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