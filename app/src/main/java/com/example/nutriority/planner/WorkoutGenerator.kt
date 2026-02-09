package com.example.nutriority.planner

import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import com.example.nutriority.ui.util.WorkoutUtil
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class WorkoutGenerator @Inject constructor() {

    enum class MovementType { PUSH, PULL, LEGS, CORE, FULL_BODY, UNKNOWN }
    enum class ExerciseRole { COMPOUND, ISOLATION, FINISHER_CORE, WARMUP, COOLDOWN }

    /**
     * Generates a complete workout based on the specific structure:
     * 1-2 Warmup, 3-5 Main Exercises, 1-2 Cooldown.
     */
    fun generatePersonalizedWorkout(
        id: Int,
        focus: String,
        difficulty: String,
        exercisePool: List<Exercise>,
        random: Random
    ): WorkoutWithExercises {
        
        val workoutName = "$difficulty $focus Tone"
        val workout = Workout(
            id = id,
            name = workoutName,
            description = "A balanced workout for toning and strengthening your $focus.",
            category = "Strength",
            targetMuscle = focus,
            imageName = "img_gym_bg", 
            difficulty = difficulty,
            tags = listOf("keep fit", "strength", "bodyweight"),
            includeWarmupCooldown = true
        )

        val assignments = mutableListOf<WorkoutExerciseWithDetail>()
        
        // 1. Filter Pools
        val warmupPool = exercisePool.filter { getExerciseRole(it) == ExerciseRole.WARMUP }
        val cooldownPool = exercisePool.filter { getExerciseRole(it) == ExerciseRole.COOLDOWN }
        val mainPool = exercisePool.filter { 
            val role = getExerciseRole(it)
            role == ExerciseRole.COMPOUND || role == ExerciseRole.ISOLATION || role == ExerciseRole.FINISHER_CORE
        }

        var order = 0

        // 2. Add Warmup (1 or 2)
        val numWarmup = random.nextInt(1, 3)
        warmupPool.shuffled(random).take(numWarmup).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "warmup", order++, isStrength = false), ex))
        }

        // 3. Add Main Exercises (3-5)
        val numMain = random.nextInt(3, 6)
        mainPool.shuffled(random).take(numMain).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "Exercise", order++, isStrength = true), ex))
        }

        // 4. Add Cooldown (1 or 2)
        val numCooldown = random.nextInt(1, 3)
        cooldownPool.shuffled(random).take(numCooldown).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "cooldown", order++, isStrength = false), ex))
        }

        // 5. Build Detail List for Duration Calculation
        workout.duration = WorkoutUtil.calculateTotalDuration(assignments, true)

        return WorkoutWithExercises(workout, assignments)
    }

    private fun createAssignment(workoutId: Int, ex: Exercise, category: String, order: Int, isStrength: Boolean): WorkoutExercise {
        return WorkoutExercise(
            workoutId = workoutId,
            exerciseId = ex.id,
            category = category,
            sets = if (isStrength) 3 else 1,
            reps = if (isStrength) "10-15" else "1",
            duration = if (isStrength) "" else "1 min",
            rest = if (isStrength) "60s" else "0s",
            order = order
        )
    }

    fun getMovementType(exercise: Exercise): MovementType {
        val target = (exercise.target + " " + exercise.bodyPart + " " + exercise.targetMuscle).lowercase()
        return when {
            target.contains("chest") || target.contains("shoulder") || target.contains("tricep") -> MovementType.PUSH
            target.contains("back") || target.contains("bicep") -> MovementType.PULL
            target.contains("quad") || target.contains("hamstring") || target.contains("glute") || target.contains("calf") || target.contains("leg") -> MovementType.LEGS
            target.contains("abs") || target.contains("oblique") || target.contains("core") -> MovementType.CORE
            else -> MovementType.UNKNOWN
        }
    }

    fun getExerciseRole(exercise: Exercise): ExerciseRole {
        val name = exercise.name.lowercase()
        val category = exercise.category.lowercase()
        
        if (category.contains("warmup")) return ExerciseRole.WARMUP
        if (category.contains("cooldown") || category.contains("stretch")) return ExerciseRole.COOLDOWN

        val warmupKeywords = listOf("jumping jack", "arm circle", "leg swing", "high knee", "march", "stretching")
        if (warmupKeywords.any { name.contains(it) }) return ExerciseRole.WARMUP

        val cooldownKeywords = listOf("cat-cow", "child's pose", "seated forward fold", "stretch", "cobra")
        if (cooldownKeywords.any { name.contains(it) }) return ExerciseRole.COOLDOWN

        val compoundKeywords = listOf("push-up", "pushup", "dip", "pull-up", "pullup", "chin-up", "chinup", "squat", "lunge", "plank", "burpee", "mountain climber")
        val isolationKeywords = listOf("raise", "crunch", "extension", "curl", "hold", "kick")

        return when {
            compoundKeywords.any { name.contains(it) } -> ExerciseRole.COMPOUND
            isolationKeywords.any { name.contains(it) } -> ExerciseRole.ISOLATION
            getMovementType(exercise) == MovementType.CORE -> ExerciseRole.FINISHER_CORE
            else -> ExerciseRole.ISOLATION
        }
    }
}
