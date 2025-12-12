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
        val allWorkouts = workoutRepository.getAllWorkoutsList().associateBy { it.workout.targetMuscle }
        val bmi = calculateBmi(user.weightKg, user.heightCm)

        val sessions = when (user.goal.lowercase()) {
            "build muscle" -> createBuildMusclePlan(allWorkouts, bmi)
            "lose weight" -> createLoseWeightPlan(allWorkouts)
            "keep fit" -> createKeepFitPlan(allWorkouts)
            else -> createKeepFitPlan(allWorkouts)
        }

        val weeklyCaloriesBurn = sessions.sumOf { it.durationMinutes } * 8
        return WorkoutPlan(weeklyCaloriesBurn, sessions)
    }

    private fun calculateBmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0) return 0.0
        return weightKg / (heightCm / 100).pow(2)
    }

    private fun createBuildMusclePlan(workouts: Map<String, WorkoutWithExercises>, bmi: Double): List<WorkoutSession> {
        val plan = if (bmi < 25) {
            // Standard Push, Pull, Legs for foundational strength
            listOf(
                workouts["Chest"],     // Push
                workouts["Back"],      // Pull
                workouts["Legs"],      // Legs
                null,                  // Rest
                workouts["Shoulders"], // Push
                workouts["Arms"],      // Pull/Accessory
                null                   // Rest
            )
        } else {
            // Higher volume for more advanced users or those with higher BMI
            listOf(
                workouts["Chest"],     // Push
                workouts["Back"],      // Pull
                workouts["Legs"],      // Legs
                workouts["Shoulders"], // Push
                workouts["Arms"],      // Pull/Accessory
                workouts["Abs"],
                null                   // Rest
            )
        }
        return createSessionsFromPlan(plan)
    }

    private fun createLoseWeightPlan(workouts: Map<String, WorkoutWithExercises>): List<WorkoutSession> {
        val plan = listOf(
            workouts["Full Body"],
            workouts["Abs"],
            workouts["Full Body"],
            null, 
            workouts["Full Body"],
            workouts["Abs"],
            null 
        )
        return createSessionsFromPlan(plan)
    }

    private fun createKeepFitPlan(workouts: Map<String, WorkoutWithExercises>): List<WorkoutSession> {
        val plan = listOf(
            workouts["Upper Body"],
            workouts["Legs"],
            null,
            workouts["Upper Body"],
            null,
            workouts["Full Body"],
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
