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

    private val weeklySchedule = listOf(
        "Full Body",  // Day 1
        "Rest Day",   // Day 2
        "Abs",        // Day 3
        "Upper Body", // Day 4
        "Rest Day",   // Day 5
        "Legs",       // Day 6
        "Core"        // Day 7
    )

    suspend fun planWorkouts(user: User): WorkoutPlan {
        workoutRepository.ensureLibraryIsLoaded()
        val allExercises = workoutRepository.getAllExercises().first()
        
        if (allExercises.isEmpty()) {
            return createErrorPlan("No exercises found. Please sync from cloud.")
        }

        val userDifficulty = mapActivityLevelToDifficulty(user.activityLevel)
        val sessions = mutableListOf<WorkoutSession>()

        val seed = user.id.hashCode().toLong()
        val random = Random(seed)

        for (week in 0 until 4) {
            weeklySchedule.forEachIndexed { dayInWeek, focus ->
                val totalDayIndex = (week * 7) + dayInWeek
                
                if (focus == "Rest Day") {
                    sessions.add(createRestDay(totalDayIndex))
                } else {
                    val workoutId = 1000 + totalDayIndex
                    
                    // SMART FILTERING
                    val pool = filterExercisesByFocus(focus, allExercises)

                    val generated = workoutGenerator.generatePersonalizedWorkout(
                        id = workoutId,
                        focus = focus,
                        difficulty = userDifficulty,
                        exercisePool = pool,
                        random = random
                    )

                    // Persist to local DB immediately
                    workoutRepository.updateWorkoutWithExercises(
                        generated.workout, 
                        generated.exerciseAssignments.map { it.assignment }
                    )

                    val durationStr = WorkoutUtil.calculateTotalDuration(generated.exerciseAssignments, true)
                    val durationMinutes = durationStr.filter { it.isDigit() }.toIntOrNull() ?: 30
                    val caloriesBurned = ((generated.workout.metValue * 3.5 * user.weightKg) / 200 * durationMinutes).toInt()

                    // Populate detailed exercise lists for the plan JSON
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

        return WorkoutPlan(sessions.sumOf { it.caloriesBurned }, sessions)
    }

    /**
     * Ensures that a plan restored from the cloud is correctly inflated into the local database.
     */
    suspend fun syncPlanToDatabase(plan: WorkoutPlan) {
        plan.sessions.forEach { session ->
            val workoutId = session.workoutDetails?.id ?: return@forEach
            
            val assignments = mutableListOf<WorkoutExercise>()
            var order = 0
            
            session.warmup?.forEach { ex -> assignments.add(createWorkoutExercise(workoutId, ex, "warmup", order++)) }
            session.exercises?.forEach { ex -> assignments.add(createWorkoutExercise(workoutId, ex, "Exercise", order++)) }
            session.cooldown?.forEach { ex -> assignments.add(createWorkoutExercise(workoutId, ex, "cooldown", order++)) }
            
            if (assignments.isNotEmpty()) {
                val workout = Workout(
                    id = workoutId,
                    name = session.focus,
                    description = session.description,
                    category = "Personalized",
                    difficulty = if (session.focus.contains("Beginner")) "Beginner" else if (session.focus.contains("Advanced")) "Advanced" else "Intermediate",
                    duration = "${session.durationMinutes} min",
                    imageName = "img_gym_bg"
                )
                workoutRepository.updateWorkoutWithExercises(workout, assignments)
            }
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

    private fun mapToPlannerExercise(assignment: com.example.nutriority.data.model.WorkoutExercise, allExercises: List<Exercise>): PlannerExercise {
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
        "Sedentary", "Lightly active" -> "Beginner"
        "Active" -> "Intermediate"
        else -> "Advanced"
    }

    private fun createErrorPlan(message: String) = WorkoutPlan(0, List(28) { index ->
        WorkoutSession(day = "Day ${index + 1}", focus = "Error", durationMinutes = 0, description = message, caloriesBurned = 0)
    })
}
