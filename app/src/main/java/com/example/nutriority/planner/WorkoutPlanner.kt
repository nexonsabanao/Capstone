package com.example.nutriority.planner

import android.app.Application
import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.repository.WorkoutRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

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
        val allWorkouts = workoutRepository.getAllWorkoutsList()
        
        if (allWorkouts.isEmpty()) {
            return createErrorPlan("No official workouts found. Please sync from cloud.")
        }

        val userDifficulty = mapActivityLevelToDifficulty(user.activityLevel)

        val sessions = weeklySchedule.mapIndexed { index, movementType ->
            if (movementType == WorkoutGenerator.MovementType.UNKNOWN) {
                createRestDay(index)
            } else {
                // Find an official workout that matches the movement type and difficulty
                val matchingWorkout = findMatchingWorkout(movementType, userDifficulty, allWorkouts)
                
                if (matchingWorkout != null) {
                    val workout = matchingWorkout.workout
                    // Calculate duration based on exercises
                    val totalDuration = (matchingWorkout.exerciseAssignments.size * 5).coerceAtLeast(20)
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
                } else {
                    // Fallback if no specific match is found for that day
                    createRestDay(index)
                }
            }
        }

        val totalWeeklyCalories = sessions.sumOf { it.caloriesBurned }
        return WorkoutPlan(totalWeeklyCalories, sessions)
    }

    private fun findMatchingWorkout(
        movementType: WorkoutGenerator.MovementType,
        difficulty: String,
        allWorkouts: List<WorkoutWithExercises>
    ): WorkoutWithExercises? {
        // Filter by difficulty first
        val diffMatch = allWorkouts.filter { it.workout.difficulty.equals(difficulty, true) }
        val pool = if (diffMatch.isNotEmpty()) diffMatch else allWorkouts

        return pool.filter { workoutWithEx ->
            val target = workoutWithEx.workout.targetMuscle.lowercase()
            when (movementType) {
                WorkoutGenerator.MovementType.PUSH -> target.contains("chest") || target.contains("shoulder") || target.contains("push")
                WorkoutGenerator.MovementType.PULL -> target.contains("back") || target.contains("arm") || target.contains("pull")
                WorkoutGenerator.MovementType.LEGS -> target.contains("leg")
                WorkoutGenerator.MovementType.CORE -> target.contains("abs") || target.contains("core")
                else -> false
            }
        }.shuffled().firstOrNull() ?: pool.shuffled().firstOrNull()
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
