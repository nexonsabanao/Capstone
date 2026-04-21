package com.example.nutriority.planner

import android.app.Application
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.ui.util.WorkoutUtil
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class WorkoutPlanner @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val workoutGenerator: WorkoutGenerator,
    private val application: Application
) {

    /**
     * Generates a 4-week focus-rotating schedule.
     * Day 3 is always Abs (replacing Core).
     * Day 4 rotates through specific upper body parts since generic "Upper Body" was removed.
     */
    private fun getFocusForDay(week: Int, dayInWeek: Int): String {
        return when (dayInWeek) {
            0 -> "Full Body"
            1 -> "Rest Day"
            2 -> "Abs"
            3 -> when (week) {
                0 -> "Chest"
                1 -> "Back"
                2 -> "Shoulders"
                else -> "Arms"
            }
            4 -> "Rest Day"
            5 -> "Legs"
            6 -> "Rest Day"
            else -> "Rest Day"
        }
    }

    suspend fun planWorkouts(user: User): WorkoutPlan {
        workoutRepository.ensureLibraryIsLoaded()
        val allExercises = workoutRepository.getAllExercises().first()
        
        if (allExercises.isEmpty()) {
            return createErrorPlan("No exercises found. Please sync from cloud.")
        }

        val userDifficulty = mapActivityLevelToDifficulty(user.activityLevel)
        val sessions = mutableListOf<WorkoutSession>()
        val allWorkouts = mutableListOf<Workout>()
        val allAssignments = mutableListOf<WorkoutExercise>()
        val usedExerciseIds = mutableSetOf<String>()

        // Use a more dynamic seed to ensure variation across users AND plans
        val seed = user.id.hashCode().toLong() + System.currentTimeMillis()
        val random = Random(seed)

        for (week in 0 until 4) {
            for (dayInWeek in 0 until 7) {
                val focus = getFocusForDay(week, dayInWeek)
                val totalDayIndex = (week * 7) + dayInWeek
                
                if (focus == "Rest Day") {
                    sessions.add(createRestDay(totalDayIndex))
                } else {
                    val workoutId = 1000 + totalDayIndex
                    val pool = filterExercisesByFocus(focus, allExercises)

                    val generated = workoutGenerator.generatePersonalizedWorkout(
                        id = workoutId,
                        focus = focus,
                        difficulty = userDifficulty,
                        exercisePool = pool,
                        random = random,
                        usedExerciseIds = usedExerciseIds
                    )
                    
                    // Track used exercises to prioritize variety in future sessions
                    usedExerciseIds.addAll(generated.exerciseAssignments.map { it.assignment.exerciseId })

                    allWorkouts.add(generated.workout)
                    allAssignments.addAll(generated.exerciseAssignments.map { it.assignment })

                    val durationStr = WorkoutUtil.calculateTotalDuration(generated.exerciseAssignments, true)
                    val durationMinutes = durationStr.filter { it.isDigit() }.toIntOrNull() ?: 30
                    
                    // Formula: Calories = (MET * 3.5 * weightKg / 200) * durationInMinutes
                    val caloriesBurned = ((generated.workout.metValue * 3.5 * user.weightKg) / 200 * durationMinutes).toInt()

                    val warmupList = generated.exerciseAssignments
                        .filter { it.assignment.category == "warmup" }
                        .map { mapToPlannerExercise(it.assignment, allExercises) }
                    
                    val mainList = generated.exerciseAssignments
                        .filter { it.assignment.category == "Exercise" }
                        .map { mapToPlannerExercise(it.assignment, allExercises) }
                    
                    val cooldownList = generated.exerciseAssignments
                        .filter { it.assignment.category == "cooldown" }
                        .map { mapToPlannerExercise(it.assignment, allExercises) }

                    sessions.add(
                        WorkoutSession(
                            day = "Day ${totalDayIndex + 1}",
                            focus = generated.workout.name,
                            durationMinutes = durationMinutes,
                            description = generated.workout.description,
                            caloriesBurned = caloriesBurned,
                            workoutDetails = WorkoutDetails(id = generated.workout.id),
                            warmup = warmupList,
                            exercises = mainList,
                            cooldown = cooldownList
                        )
                    )
                }
            }
        }

        workoutRepository.updateWorkoutsWithExercises(allWorkouts, allAssignments)

        return WorkoutPlan(sessions.sumOf { it.caloriesBurned }, sessions)
    }

    suspend fun syncPlanToDatabase(plan: WorkoutPlan) {
        val workouts = mutableListOf<Workout>()
        val assignments = mutableListOf<WorkoutExercise>()
        
        plan.sessions.forEach { session ->
            val workoutId = session.workoutDetails?.id ?: return@forEach
            
            var order = 0
            session.warmup?.forEach { ex -> assignments.add(createWorkoutExercise(workoutId, ex, "warmup", order++)) }
            session.exercises?.forEach { ex -> assignments.add(createWorkoutExercise(workoutId, ex, "Exercise", order++)) }
            session.cooldown?.forEach { ex -> assignments.add(createWorkoutExercise(workoutId, ex, "cooldown", order++)) }
            
            workouts.add(Workout(
                id = workoutId,
                name = session.focus,
                description = session.description,
                category = "Personalized",
                difficulty = if (session.focus.contains("Beginner")) "Beginner" else if (session.focus.contains("Advanced")) "Advanced" else "Intermediate",
                duration = "${session.durationMinutes} min",
                imageName = "img_gym_bg"
            ))
        }
        
        if (workouts.isNotEmpty()) {
            workoutRepository.updateWorkoutsWithExercises(workouts, assignments)
        }
    }

    private fun createWorkoutExercise(workoutId: Int, pe: PlannerExercise, category: String, order: Int): WorkoutExercise {
        return WorkoutExercise(
            workoutId = workoutId,
            exerciseId = pe.exerciseId,
            category = category,
            sets = pe.sets,
            reps = pe.reps,
            duration = pe.duration,
            rest = pe.rest,
            order = order
        )
    }

    private fun filterExercisesByFocus(focus: String, allExercises: List<Exercise>): List<Exercise> {
        val focusLower = focus.lowercase()
        return allExercises.filter { ex ->
            val target = ex.target.lowercase()
            val bodyPart = ex.bodyPart.lowercase()
            val muscle = ex.targetMuscle.lowercase()
            val category = ex.category.lowercase()

            val isGeneralWarmup = category.contains("warmup")
            val isGeneralCooldown = category.contains("cooldown") || category.contains("stretch")

            val isFocusMatch = when (focusLower) {
                "full body" -> true 
                "upper body" -> bodyPart.contains("arm") || bodyPart.contains("chest") || 
                               bodyPart.contains("back") || bodyPart.contains("shoulder") ||
                               bodyPart.contains("waist") || bodyPart.contains("neck")
                "legs" -> bodyPart.contains("leg") || target.contains("quad") || 
                         target.contains("glute") || target.contains("hamstring")
                "abs", "core" -> bodyPart.contains("waist") || target.contains("abs") || target.contains("core")
                else -> bodyPart.contains(focusLower) || target.contains(focusLower) || muscle.contains(focusLower)
            }

            isFocusMatch || isGeneralWarmup || isGeneralCooldown
        }
    }

    private fun mapToPlannerExercise(assignment: WorkoutExercise, allExercises: List<Exercise>): PlannerExercise {
        val exercise = allExercises.find { it.id == assignment.exerciseId }
        return PlannerExercise(
            name = exercise?.name ?: "Unknown Exercise",
            exerciseId = assignment.exerciseId,
            sets = assignment.sets,
            reps = assignment.reps,
            duration = assignment.duration,
            rest = assignment.rest
        )
    }

    private fun createRestDay(index: Int) = WorkoutSession(
        day = "Day ${index + 1}",
        focus = "Rest Day",
        durationMinutes = 0,
        description = "Recovery day. Let your muscles rebuild.",
        caloriesBurned = 0
    )

    private fun mapActivityLevelToDifficulty(activityLevel: String) = when (activityLevel) {
        "Sedentary" -> "Beginner"
        "Lightly active" -> "Intermediate"
        "Active" -> "Advanced"
        else -> "Beginner"
    }

    private fun createErrorPlan(message: String) = WorkoutPlan(0, List(28) { index ->
        WorkoutSession(day = "Day ${index + 1}", focus = "Error", durationMinutes = 0, description = message, caloriesBurned = 0)
    })
}
