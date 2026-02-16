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
     * Generates a complete workout based on the specific structure.
     * Dynamic counts and intensities based on difficulty (Activity Level).
     * @param usedExerciseIds Set of IDs that have already been used in the current plan to prioritize variety.
     */
    fun generatePersonalizedWorkout(
        id: Int,
        focus: String,
        difficulty: String,
        exercisePool: List<Exercise>,
        random: Random,
        usedExerciseIds: Set<String> = emptySet()
    ): WorkoutWithExercises {
        
        val workoutName = "$difficulty $focus Routine"
        val workout = Workout(
            id = id,
            name = workoutName,
            description = "A precision-engineered $difficulty workout targeting $focus.",
            category = "Strength",
            targetMuscle = focus,
            imageName = "img_gym_bg", 
            difficulty = difficulty,
            tags = listOf("personalized", "strength", "fitness"),
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

        // 2. Add Warmup (Smart Scaling)
        val numWarmup = if (difficulty == "Advanced") 3 else 2
        pickExercises(warmupPool, numWarmup, usedExerciseIds, random).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "warmup", order++, difficulty), ex))
        }

        // 3. Add Main Exercises (Genius Scaling)
        val numMain = when (difficulty) {
            "Beginner" -> random.nextInt(3, 4)
            "Intermediate" -> random.nextInt(4, 6)
            "Advanced" -> random.nextInt(5, 7)
            else -> 4
        }
        
        pickExercises(mainPool, numMain, usedExerciseIds, random).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "Exercise", order++, difficulty), ex))
        }

        // 4. Add Cooldown (Smart Scaling)
        val numCooldown = if (difficulty == "Advanced") 1 else 2
        pickExercises(cooldownPool, numCooldown, usedExerciseIds, random).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "cooldown", order++, difficulty), ex))
        }

        // 5. Update Duration
        workout.duration = WorkoutUtil.calculateTotalDuration(assignments, true)

        return WorkoutWithExercises(workout, assignments)
    }

    /**
     * Prioritizes unused exercises while maintaining randomness.
     */
    private fun pickExercises(
        pool: List<Exercise>,
        count: Int,
        usedIds: Set<String>,
        random: Random
    ): List<Exercise> {
        if (pool.isEmpty()) return emptyList()
        
        val unused = pool.filter { !usedIds.contains(it.id) }.shuffled(random)
        val used = pool.filter { usedIds.contains(it.id) }.shuffled(random)
        
        // Combine them: Unused first, then used if we need more
        return (unused + used).take(count)
    }

    private fun createAssignment(workoutId: Int, ex: Exercise, category: String, order: Int, difficulty: String): WorkoutExercise {
        val isMain = category == "Exercise"
        
        val sets = when {
            !isMain -> 1
            difficulty == "Beginner" -> 2
            difficulty == "Intermediate" -> 3
            difficulty == "Advanced" -> 4
            else -> 3
        }

        val reps = when {
            !isMain -> "1"
            difficulty == "Beginner" -> "8-12"
            difficulty == "Intermediate" -> "12-15"
            difficulty == "Advanced" -> "15-20"
            else -> "10-12"
        }

        val rest = when {
            !isMain -> "0s"
            difficulty == "Beginner" -> "90s"
            difficulty == "Intermediate" -> "60s"
            difficulty == "Advanced" -> "45s"
            else -> "60s"
        }

        val duration = when {
            isMain -> ""
            difficulty == "Beginner" -> "1 min"
            difficulty == "Intermediate" -> "2 min"
            difficulty == "Advanced" -> "3 min"
            else -> "1 min"
        }

        return WorkoutExercise(
            workoutId = workoutId,
            exerciseId = ex.id,
            category = category,
            sets = sets,
            reps = reps,
            duration = duration,
            rest = rest,
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
