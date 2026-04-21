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
        val numWarmup = 2
        pickExercises(warmupPool, numWarmup, usedExerciseIds, random).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "warmup", order++, difficulty, random), ex))
        }

        // 3. Add Main Exercises (Genius Scaling)
        val numMain = when (difficulty) {
            "Beginner" -> random.nextInt(4, 6)
            "Intermediate" -> random.nextInt(5, 7)
            "Advanced" -> random.nextInt(6, 8)
            else -> 5
        }
        
        pickExercises(mainPool, numMain, usedExerciseIds, random).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "Exercise", order++, difficulty, random), ex))
        }

        // 4. Add Cooldown (Smart Scaling)
        val numCooldown = random.nextInt(1, 3)
        pickExercises(cooldownPool, numCooldown, usedExerciseIds, random).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "cooldown", order++, difficulty, random), ex))
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

    private fun createAssignment(workoutId: Int, ex: Exercise, category: String, order: Int, difficulty: String, random: Random): WorkoutExercise {
        val isMain = category == "Exercise"
        
        // BUG FIX: Inject variation into sets/reps based on difficulty to avoid identical daily stats
        val sets = when {
            !isMain -> 1
            difficulty == "Beginner" -> random.nextInt(2, 4) // 2-3
            difficulty == "Intermediate" -> random.nextInt(3, 5) // 3-4
            difficulty == "Advanced" -> random.nextInt(4, 6) // 4-5
            else -> 3
        }

        val reps = when {
            !isMain -> "1"
            difficulty == "Beginner" -> "${random.nextInt(8, 11)}-${random.nextInt(12, 14)}"
            difficulty == "Intermediate" -> "${random.nextInt(10, 13)}-${random.nextInt(15, 17)}"
            difficulty == "Advanced" -> "${random.nextInt(12, 16)}-${random.nextInt(18, 21)}"
            else -> "10-12"
        }

        val rest = when {
            !isMain -> "0s"
            difficulty == "Beginner" -> "${random.nextInt(60, 91)}s"
            difficulty == "Intermediate" -> "${random.nextInt(45, 61)}s"
            difficulty == "Advanced" -> "${random.nextInt(30, 46)}s"
            else -> "60s"
        }

        val duration = when {
            isMain -> ""
            category == "warmup" -> "${random.nextInt(1, 3)} min"
            category == "cooldown" -> "${random.nextInt(1, 3)} min"
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
