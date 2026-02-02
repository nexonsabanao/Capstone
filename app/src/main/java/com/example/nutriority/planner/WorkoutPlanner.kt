package com.example.nutriority.planner

import android.app.Application
import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.repository.WorkoutRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class WorkoutPlanner @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val workoutGenerator: WorkoutGenerator,
    private val application: Application,
    private val gson: Gson
) {

    // Fixed 7-day schedule with recovery
    private val weeklySchedule = listOf(
        WorkoutGenerator.MovementType.PUSH,
        WorkoutGenerator.MovementType.UNKNOWN, // Rest
        WorkoutGenerator.MovementType.PULL,
        WorkoutGenerator.MovementType.UNKNOWN, // Rest
        WorkoutGenerator.MovementType.LEGS,
        WorkoutGenerator.MovementType.UNKNOWN, // Rest
        WorkoutGenerator.MovementType.CORE
    )

    suspend fun planWorkouts(user: User): WorkoutPlan {
        workoutRepository.ensureLibraryIsLoaded()
        val allExercises = workoutRepository.getAllExercises().first()
        if (allExercises.isEmpty()) {
            return createErrorPlan("No exercises found in the database.")
        }

        val userDifficulty = mapActivityLevelToDifficulty(user.activityLevel)

        val sessions = weeklySchedule.mapIndexed { index, movementType ->
            if (movementType == WorkoutGenerator.MovementType.UNKNOWN) {
                createRestDay(index)
            } else {
                val workoutWithExercises = workoutGenerator.generateStructuredWorkout(
                    movementType = movementType,
                    difficulty = userDifficulty,
                    allExercises = allExercises
                )
                
                val workout = workoutWithExercises.workout
                val totalDuration = (workoutWithExercises.exerciseAssignments.size * 6).coerceAtLeast(15)
                val caloriesBurned = ((workout.metValue * 3.5 * user.weightKg) / 200 * totalDuration).toInt()

                WorkoutSession(
                    day = "Day ${index + 1}",
                    focus = workout.name,
                    durationMinutes = totalDuration,
                    description = workout.description,
                    caloriesBurned = caloriesBurned,
                    workoutDetails = WorkoutDetails(id = workout.id, sets = null, reps = null),
                    sets = null,
                    reps = null
                )
            }
        }

        val totalWeeklyCalories = sessions.sumOf { it.caloriesBurned }
        return WorkoutPlan(totalWeeklyCalories, sessions)
    }

    private fun createRestDay(index: Int): WorkoutSession {
        return WorkoutSession(
            day = "Day ${index + 1}",
            focus = "Rest Day",
            durationMinutes = 0,
            description = "A day to recover and let your muscles rebuild. Recovery is where the growth happens!",
            caloriesBurned = 0
        )
    }

    private fun mapActivityLevelToDifficulty(activityLevel: String): String {
        return when (activityLevel) {
            "Sedentary", "Lightly Active", "Lightly active" -> "Beginner"
            "Active" -> "Intermediate"
            "Very active" -> "Advanced"
            else -> "Beginner"
        }
    }

    private fun calculateBmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0) return 0.0
        return weightKg / (heightCm / 100).pow(2)
    }

    private fun createErrorPlan(message: String): WorkoutPlan {
        val errorSessions = List(7) { index ->
            WorkoutSession(
                day = "Day ${index + 1}",
                focus = "Error",
                durationMinutes = 0,
                description = message,
                caloriesBurned = 0
            )
        }
        return WorkoutPlan(0, errorSessions)
    }
}
