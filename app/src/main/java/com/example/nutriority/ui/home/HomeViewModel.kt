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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

    val allMeals: StateFlow<List<Meal>?>
    val allWorkouts: StateFlow<List<Workout>?> 
    val unfilteredWorkouts: StateFlow<List<Workout>> 
    val allArticles: StateFlow<List<Article>>
    private val _isDataReady = MutableStateFlow(false)
    val isDataReady: StateFlow<Boolean> = _isDataReady
    val calorieGoal: StateFlow<String>

    private val _navigateToLogin = MutableSharedFlow<Unit>(replay = 1)
    val navigateToLogin: SharedFlow<Unit> = _navigateToLogin

    init {
        // Start real-time Firestore listeners
        mealRepository.startRealtimeMealSync(viewModelScope)
        articleRepository.startRealtimeArticleSync(viewModelScope)
        workoutRepository.startRealtimeExerciseSync(viewModelScope)
        workoutRepository.startRealtimeCustomWorkoutSync(viewModelScope)
        userRepository.startRealtimeUserSync(viewModelScope)

        viewModelScope.launch {
            // Force a direct cloud status check first (stronger than waiting for sync)
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                    if (doc.getString("status") == "deleted") {
                        handleSessionExpired()
                        return@launch
                    }
                }
            } catch (_: Exception) {}

            checkUserSession()
            
            launch { try { articleRepository.syncArticlesFromCloud() } catch (_: Exception) {} }
            launch { try { mealRepository.syncMealsFromCloud() } catch (_: Exception) {} }
            launch { try { workoutRepository.syncExercisesFromCloud() } catch (_: Exception) {} }
            launch { try { recommendedWorkoutRepository.syncOfficialWorkoutsFromCloud() } catch (_: Exception) {} }
            
            delay(500)
            _isDataReady.value = true
        }

        allMeals = combine(mealRepository.allMeals, userRepository.getUser) { meals, user ->
            if (user == null || meals.isEmpty()) return@combine meals

            var filtered = if (user.preferredDiet.isNotEmpty() && !user.preferredDiet.equals("Balanced", ignoreCase = true)) {
                meals.filter { it.preferredDiet.equals(user.preferredDiet, ignoreCase = true) }
            } else {
                meals
            }

            if (filtered.isEmpty()) {
                filtered = meals
            }

            // Improved Filtering by Excluded Ingredients
            if (user.excludedIngredients.isNotEmpty()) {
                filtered = filtered.filter { meal ->
                    user.excludedIngredients.none { excluded ->
                        val normalizedExcluded = normalizeIngredient(excluded)
                        meal.ingredients.any { ingredient -> 
                            val normalizedIngredient = normalizeIngredient(ingredient)
                            normalizedIngredient.contains(normalizedExcluded, ignoreCase = true) ||
                            normalizedExcluded.contains(normalizedIngredient, ignoreCase = true)
                        }
                    }
                }
            }

            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val timePriority = when (currentHour) {
                in 5..10 -> listOf("Breakfast", "Lunch", "Dinner")
                in 11..15 -> listOf("Lunch", "Dinner", "Breakfast")
                in 16..21 -> listOf("Dinner", "Breakfast", "Lunch")
                else -> listOf("Breakfast", "Lunch", "Dinner")
            }

            filtered.sortedWith(compareBy<Meal> { meal ->
                val index = timePriority.indexOfFirst { it.equals(meal.mealTime, ignoreCase = true) }
                if (index == -1) 99 else index
            }.thenBy { it.name })

        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
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
            if (user == null || workouts.isEmpty()) return@combine emptyList()
            
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
            initialValue = null
        )

        allWorkouts = recommendedWorkouts

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
        
        // Listen for user changes to catch "deleted" status in real-time
        viewModelScope.launch {
            userRepository.getUser.collect { user ->
                if (user?.status == "deleted") {
                    handleSessionExpired()
                }
            }
        }
    }

    private suspend fun checkUserSession() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            _navigateToLogin.emit(Unit)
            return
        }

        val localUser = userRepository.getInitialUser()
        if (localUser?.status == "deleted") {
            handleSessionExpired()
        }
    }

    private suspend fun handleSessionExpired() {
        // Use NonCancellable to ensure cleanup completes even if system tries to kill the coroutine
        withContext(NonCancellable) {
            userRepository.deleteAll()
            FirebaseAuth.getInstance().signOut()
            _navigateToLogin.emit(Unit)
        }
    }

    private fun normalizeIngredient(input: String): String {
        val lower = input.lowercase().trim()
        return if (lower.endsWith("s") && lower.length > 3) {
            lower.dropLast(1)
        } else {
            lower
        }
    }
}
