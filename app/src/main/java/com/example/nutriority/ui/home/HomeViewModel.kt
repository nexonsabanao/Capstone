package com.example.nutriority.ui.home

import androidx.lifecycle.ViewModel
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
import com.example.nutriority.ui.util.AgeUtil
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

        allMeals = combine(mealRepository.allMeals, userRepository.getUser) { meals, user ->
            if (user == null || meals.isEmpty()) return@combine emptyList<Meal>()

            var filtered = if (user.preferredDiet.isNotEmpty() && user.preferredDiet != "Balanced") {
                meals.filter { it.preferredDiet.equals(user.preferredDiet, ignoreCase = true) }
            } else {
                meals
            }

            if (user.excludedIngredients.isNotEmpty()) {
                filtered = filtered.filter { meal ->
                    user.excludedIngredients.none { excluded ->
                        meal.ingredients.any { ingredient -> 
                            ingredient.contains(excluded, ignoreCase = true) 
                        }
                    }
                }
            }

            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val timePriority = when {
                currentHour in 5..10 -> listOf("Breakfast", "Lunch", "Dinner")
                currentHour in 11..15 -> listOf("Lunch", "Dinner", "Breakfast")
                currentHour in 16..21 -> listOf("Dinner", "Breakfast", "Lunch")
                else -> listOf("Breakfast", "Lunch", "Dinner") 
            }

            filtered.sortedWith(compareBy<Meal> { meal ->
                val index = timePriority.indexOfFirst { it.equals(meal.mealTime, ignoreCase = true) }
                if (index == -1) 99 else index
            }.thenBy { it.name })

        }.stateIn(
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
            userRepository.getUser
        ) { workouts, user ->
            if (user == null || workouts.isEmpty()) return@combine emptyList<Workout>()
            
            val stableWorkouts = workouts.sortedBy { it.id }
            val officialOnly = stableWorkouts.filter { it.id in 1..25 }
            
            val desiredDifficulties = when (user.activityLevel) {
                "Sedentary", "Lightly active" -> listOf("Beginner")
                "Active" -> listOf("Beginner", "Intermediate")
                else -> listOf("Intermediate", "Advanced")
            }

            val filteredWorkouts = officialOnly.filter { it.difficulty in desiredDifficulties }

            val seed = Calendar.getInstance().get(Calendar.DAY_OF_YEAR).toLong()
            val random = Random(seed)

            if (filteredWorkouts.isEmpty()) {
                officialOnly.shuffled(random).take(5)
            } else {
                filteredWorkouts.groupBy { it.targetMuscle }
                    .map { it.value.random(random) }
                    .sortedBy { it.id }
            }
        }
        .distinctUntilChanged()
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

        calorieGoal = userRepository.getUser.map { user ->
            if (user == null) return@map "1800-2200 kcal / day"
            
            NutritionCalculator.getCalorieRangeForDisplay(
                user.weightKg,
                user.heightCm,
                AgeUtil.calculateAge(user.birthDate),
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
