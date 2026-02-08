package com.example.nutriority.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.Article
import com.example.nutriority.data.model.Meal
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.repository.ArticleRepository
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.RecommendedWorkoutRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.planner.NutritionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository,
    private val recommendedWorkoutRepository: RecommendedWorkoutRepository
) : ViewModel() {

    val allMeals: StateFlow<List<Meal>>
    val allWorkouts: StateFlow<List<Workout>> 
    val unfilteredWorkouts: StateFlow<List<Workout>> 
    val allArticles: StateFlow<List<Article>>
    val isDataReady: StateFlow<Boolean>
    val calorieGoal: StateFlow<String>

    init {
        viewModelScope.launch {
            articleRepository.syncArticlesFromCloud()
            mealRepository.syncMealsFromCloud()
            workoutRepository.syncExercisesFromCloud()
            recommendedWorkoutRepository.syncOfficialWorkoutsFromCloud()
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

        val recommendedWorkouts = combine(
            workoutRepository.allWorkouts, 
            userRepository.getUser.asFlow()
        ) { workouts, user ->
            if (user == null || workouts.isEmpty()) return@combine emptyList()
            
            // 1. Sort workouts by ID to ensure stable input for selection
            val stableWorkouts = workouts.sortedBy { it.id }
            
            // 2. STRICT FILTER: Only show official trainer workouts (ID 1-25)
            val officialOnly = stableWorkouts.filter { it.id in 1..25 }
            
            val desiredDifficulties = when (user.activityLevel) {
                "Sedentary", "Lightly active" -> listOf("Beginner")
                "Active" -> listOf("Beginner", "Intermediate")
                else -> listOf("Intermediate", "Advanced")
            }

            val filteredWorkouts = officialOnly.filter { it.difficulty in desiredDifficulties }

            // Use a stable seed (current day) to keep recommendations consistent for the day
            val seed = Calendar.getInstance().get(Calendar.DAY_OF_YEAR).toLong()
            val random = Random(seed)

            if (filteredWorkouts.isEmpty()) {
                officialOnly.shuffled(random).take(5)
            } else {
                // Group by target muscle and pick a stable selection for the day
                filteredWorkouts.groupBy { it.targetMuscle }
                    .map { it.value.random(random) }
                    .sortedBy { it.id } // Ensure consistent UI order
            }
        }
        .distinctUntilChanged() // Crucial: Only emit if the actual list content changed
        .stateIn(
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
