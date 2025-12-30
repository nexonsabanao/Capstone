package com.example.nutriority.planner

import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.repository.WorkoutRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class WorkoutPlanner @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {

    suspend fun planWorkouts(user: User): WorkoutPlan {
        val allWorkouts = workoutRepository.getAllWorkoutsList()

        if (allWorkouts.isEmpty()) {
            val errorSessions = List(7) { index ->
                WorkoutSession(
                    day = "Day ${index + 1}",
                    focus = "Error",
                    durationMinutes = 0,
                    description = "Could not load workout data. Please check that the app is updated with the latest 'workouts.json' file."
                )
            }
            return WorkoutPlan(0, errorSessions)
        }
        
        // 1. Incorporate User's Fitness Level
        val userDifficulty = mapActivityLevelToDifficulty(user.activityLevel)
        
        // Filter workouts by the user's fitness level.
        var suitableWorkouts = allWorkouts.filter { it.workout.difficulty.equals(userDifficulty, ignoreCase = true) }

        // Fallback to a lower difficulty if no workouts are found for the user's level.
        if (suitableWorkouts.isEmpty()) {
            val fallbackDifficulty = when(userDifficulty) {
                "Advanced" -> "Intermediate"
                "Intermediate" -> "Beginner"
                else -> "Beginner"
            }
            suitableWorkouts = allWorkouts.filter { it.workout.difficulty.equals(fallbackDifficulty, ignoreCase = true) }
        }

        // 2. Group workouts to allow for variety
        val workoutsByTargetMuscle = suitableWorkouts.groupBy { it.workout.targetMuscle }

        val bmi = calculateBmi(user.weightKg, user.heightCm)

        val sessions = when (user.goal.lowercase().trim()) {
            "build muscle" -> createBuildMusclePlan(workoutsByTargetMuscle, bmi)
            "lose weight" -> createLoseWeightPlan(workoutsByTargetMuscle)
            "keep fit" -> createKeepFitPlan(workoutsByTargetMuscle)
            else -> createKeepFitPlan(workoutsByTargetMuscle)
        }

        val weeklyCaloriesBurn = sessions.sumOf { it.durationMinutes } * 8
        return WorkoutPlan(weeklyCaloriesBurn, sessions)
    }

    private fun mapActivityLevelToDifficulty(activityLevel: String): String {
        return when (activityLevel) {
            "Sedentary", "Lightly active" -> "Beginner"
            "Moderately active" -> "Intermediate"
            "Very active", "Extra active" -> "Advanced"
            else -> "Beginner" // Default to beginner if activity level is unknown
        }
    }

    private fun calculateBmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0) return 0.0
        return weightKg / (heightCm / 100).pow(2)
    }

    private fun createBuildMusclePlan(workouts: Map<String, List<WorkoutWithExercises>>, bmi: Double): List<WorkoutSession> {
        val plan = if (bmi < 25) {
            // Standard Push, Pull, Legs for foundational strength
            listOf(
                workouts["Chest"]?.randomOrNull(),     // Push
                workouts["Back"]?.randomOrNull(),      // Pull
                workouts["Legs"]?.randomOrNull(),      // Legs
                null,                                  // Rest
                workouts["Shoulders"]?.randomOrNull(), // Push
                workouts["Arms"]?.randomOrNull(),      // Pull/Accessory
                null                                   // Rest
            )
        } else {
            // Higher volume with a mid-week rest day for recovery and muscle growth
            listOf(
                workouts["Upper Body"]?.randomOrNull(), // Day 1: Upper Body
                workouts["Legs"]?.randomOrNull(),       // Day 2: Legs
                workouts["Abs"]?.randomOrNull(),        // Day 3: Core
                null,                                   // Day 4: Rest
                workouts["Upper Body"]?.randomOrNull(), // Day 5: Upper Body
                workouts["Legs"]?.randomOrNull(),       // Day 6: Legs
                workouts["Full Body"]?.randomOrNull()   // Day 7: Full Body for extra volume
            )
        }
        return createSessionsFromPlan(plan)
    }

    private fun createLoseWeightPlan(workouts: Map<String, List<WorkoutWithExercises>>): List<WorkoutSession> {
        val plan = listOf(
            workouts["Full Body"]?.randomOrNull(),
            workouts["Abs"]?.randomOrNull(),
            workouts["Full Body"]?.randomOrNull(),
            null, 
            workouts["Full Body"]?.randomOrNull(),
            workouts["Abs"]?.randomOrNull(),
            null 
        )
        return createSessionsFromPlan(plan)
    }

    private fun createKeepFitPlan(workouts: Map<String, List<WorkoutWithExercises>>): List<WorkoutSession> {
        val plan = listOf(
            workouts["Upper Body"]?.randomOrNull(),
            workouts["Legs"]?.randomOrNull(),
            null,
            workouts["Upper Body"]?.randomOrNull(),
            null,
            workouts["Full Body"]?.randomOrNull(),
            null
        )
        return createSessionsFromPlan(plan)
    }

    private fun createSessionsFromPlan(plan: List<WorkoutWithExercises?>): List<WorkoutSession> {
        return plan.mapIndexed { index, workoutData ->
            if (workoutData != null) {
                WorkoutSession(
                    day = "Day ${index + 1}",
                    focus = workoutData.workout.name,
                    durationMinutes = workoutData.workout.duration.filter { it.isDigit() }.toIntOrNull() ?: 45,
                    description = workoutData.workout.description,
                    workoutDetails = WorkoutDetails(id = workoutData.workout.id)
                )
            } else {
                WorkoutSession(
                    day = "Day ${index + 1}",
                    focus = "Rest Day",
                    durationMinutes = 0,
                    description = "A day to recover and let your muscles rebuild."
                )
            }
        }
    }
}
