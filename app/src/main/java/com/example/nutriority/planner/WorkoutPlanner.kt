package com.example.nutriority.planner

import android.app.Application
import android.util.Log
import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.repository.WorkoutRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class WorkoutPlanner @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val application: Application,
    private val gson: Gson
) {

    data class PlanConfig(
        val goal: String,
        val schedule: Map<String, List<String?>>,
        val sets: Int,
        val reps: String
    )

    suspend fun planWorkouts(user: User): WorkoutPlan {
        Log.d("WorkoutPlanner", "Planning progressive workouts for user goal: ${user.goal}")
        val allWorkouts = workoutRepository.getAllWorkoutsList()

        if (allWorkouts.isEmpty()) {
            return createErrorPlan()
        }

        val userDifficulty = mapActivityLevelToDifficulty(user.activityLevel)
        var suitableWorkouts = allWorkouts.filter { it.workout.difficulty.equals(userDifficulty, ignoreCase = true) }

        if (suitableWorkouts.isEmpty()) {
            suitableWorkouts = allWorkouts.filter { it.workout.difficulty.equals(fallbackDifficulty(userDifficulty), ignoreCase = true) }
        }

        val bmi = calculateBmi(user.weightKg, user.heightCm)
        val workoutHistory = workoutRepository.getWorkoutLogs().firstOrNull() ?: emptyList()

        // Load plans from JSON
        val plans: List<PlanConfig> = loadPlansFromJson()
        val config = plans.find { it.goal.equals(user.goal, ignoreCase = true) } ?: plans.first()

        val schedule = if (bmi > 25 && config.schedule.containsKey("bmi_high")) {
            config.schedule["bmi_high"]!!
        } else {
            config.schedule["default"]!!
        }

        val plan = buildWeeklyPlan(schedule, suitableWorkouts, workoutHistory)
        val sessions = createSessionsFromPlan(plan, config, workoutHistory, user.weightKg)

        val refinedWeeklyCalories = sessions.sumOf { it.caloriesBurned }

        return WorkoutPlan(refinedWeeklyCalories, sessions)
    }

    private fun loadPlansFromJson(): List<PlanConfig> {
        return try {
            val jsonString = application.assets.open("plans.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<PlanConfig>>() {}.type
            gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapActivityLevelToDifficulty(activityLevel: String): String {
        return when (activityLevel) {
            "Sedentary" -> "Beginner"
            "Lightly Active" -> "Intermediate"
            "Active" -> "Advanced"
            else -> "Beginner"
        }
    }

    private fun fallbackDifficulty(current: String): String {
        return when(current) {
            "Advanced" -> "Intermediate"
            "Intermediate" -> "Beginner"
            else -> "Beginner"
        }
    }

    private fun calculateBmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0) return 0.0
        return weightKg / (heightCm / 100).pow(2)
    }

    private fun buildWeeklyPlan(
        schedule: List<String?>,
        allSuitableWorkouts: List<WorkoutWithExercises>,
        history: List<WorkoutLog>
    ): List<WorkoutWithExercises?> {
        val weeklyPlan = mutableListOf<WorkoutWithExercises?>()
        val usedWorkoutIds = mutableSetOf<Int>()
        val trainedMusclesThisWeek = mutableSetOf<String>()

        // GENIUS VARIETY: Group workouts by how many times the user has completed them.
        // This ensures a perfect rotation through the library.
        val completionCounts = history.groupBy { it.workoutId }.mapValues { it.value.size }

        // Sort pool by completion count (least performed workouts first), then shuffle within those groups
        val availablePool = allSuitableWorkouts.sortedBy { completionCounts[it.workout.id] ?: 0 }
            .toMutableList()

        for (focusMuscle in schedule) {
            if (focusMuscle == null) {
                weeklyPlan.add(null)
                trainedMusclesThisWeek.clear()
                continue
            }

            // Find workout that matches muscle group and hasn't been used yet THIS week
            var chosen = availablePool.firstOrNull { workout ->
                val muscles = workout.workout.targetMuscle.lowercase()
                workout.workout.id !in usedWorkoutIds &&
                        muscles.contains(focusMuscle.lowercase())
            }

            // Fallback: If no match for that muscle, just take the least performed workout remaining
            if (chosen == null) {
                chosen = availablePool.firstOrNull { it.workout.id !in usedWorkoutIds }
            }

            weeklyPlan.add(chosen)
            chosen?.let { workout ->
                usedWorkoutIds.add(workout.workout.id)
                workout.workout.targetMuscle.split(",").forEach { muscle ->
                    trainedMusclesThisWeek.add(muscle.trim().lowercase())
                }
            }
        }
        return weeklyPlan
    }

    private fun createSessionsFromPlan(
        plan: List<WorkoutWithExercises?>,
        config: PlanConfig,
        history: List<WorkoutLog>,
        userWeight: Double
    ): List<WorkoutSession> {
        val minTargetRep = config.reps.split("-").firstOrNull()?.trim()?.toIntOrNull() ?: 8
        val maxTargetRep = config.reps.split("-").lastOrNull()?.trim()?.toIntOrNull() ?: 12

        return plan.mapIndexed { index, workoutData ->
            if (workoutData != null) {
                val lastLog = history.filter { it.workoutId == workoutData.workout.id }
                    .maxByOrNull { it.date }

                val (sets, reps) = if (lastLog != null) {
                    val lastRepsList = lastLog.reps.split(",").mapNotNull { it.trim().toIntOrNull() }
                    val lastAvgRep = lastRepsList.average().toInt()

                    var nextRep = lastAvgRep + 1
                    var nextSets = lastRepsList.size

                    if (nextRep > maxTargetRep) {
                        nextRep = minTargetRep
                        nextSets += 1
                    }

                    val newRepsString = List(nextSets) { nextRep }.joinToString(", ")
                    Pair(nextSets, newRepsString)
                } else {
                    val initialReps = List(config.sets) { minTargetRep }.joinToString(", ")
                    Pair(config.sets, initialReps)
                }

                val duration = workoutData.workout.duration.filter { it.isDigit() }.toIntOrNull() ?: 45
                val caloriesBurned = ((workoutData.workout.metValue * 3.5 * userWeight) / 200 * duration).toInt()

                WorkoutSession(
                    day = "Day ${index + 1}",
                    focus = workoutData.workout.name,
                    durationMinutes = duration,
                    description = workoutData.workout.description,
                    caloriesBurned = caloriesBurned,
                    workoutDetails = WorkoutDetails(id = workoutData.workout.id, sets = sets, reps = reps),
                    sets = sets,
                    reps = reps
                )
            } else {
                WorkoutSession(
                    day = "Day ${index + 1}",
                    focus = "Rest Day",
                    durationMinutes = 0,
                    description = "A day to recover and let your muscles rebuild.",
                    caloriesBurned = 0
                )
            }
        }
    }

    private fun createErrorPlan(): WorkoutPlan {
        val errorSessions = List(7) { index ->
            WorkoutSession(
                day = "Day ${index + 1}",
                focus = "Error",
                durationMinutes = 0,
                description = "Could not load workout data.",
                caloriesBurned = 0
            )
        }
        return WorkoutPlan(0, errorSessions)
    }
}
