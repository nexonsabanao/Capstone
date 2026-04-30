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

        // Determine the Movement Type of the focus to better align warmup/cooldown
        val focusMovement = getMovementTypeFromFocus(focus)

        // 2. Add Warmup (Filter by focus and movement type)
        val numWarmup = 2
        val filteredWarmup = filterByFocusAndMovement(warmupPool, focus, focusMovement)
        pickExercises(filteredWarmup.ifEmpty { warmupPool.filter { isUniversalWarmup(it) } }, numWarmup, usedExerciseIds, random).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "warmup", order++, difficulty, random), ex))
        }

        // 3. Add Main Exercises
        val mainFiltered = filterByFocusAndMovement(mainPool, focus, focusMovement)
        val poolToUse = if (mainFiltered.isNotEmpty()) mainFiltered else mainPool
        
        val numMain = when (difficulty) {
            "Beginner" -> random.nextInt(4, 6)
            "Intermediate" -> random.nextInt(5, 7)
            "Advanced" -> random.nextInt(6, 8)
            else -> 5
        }

        pickExercises(poolToUse, numMain, usedExerciseIds, random).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "Exercise", order++, difficulty, random), ex))
        }

        // 4. Add Cooldown (Filter by focus and movement type)
        val numCooldown = random.nextInt(1, 3)
        val filteredCooldown = filterByFocusAndMovement(cooldownPool, focus, focusMovement)
        pickExercises(filteredCooldown.ifEmpty { cooldownPool.filter { isUniversalWarmup(it) } }, numCooldown, usedExerciseIds, random).forEach { ex ->
            assignments.add(WorkoutExerciseWithDetail(createAssignment(id, ex, "cooldown", order++, difficulty, random), ex))
        }

        // 5. Update Duration
        workout.duration = WorkoutUtil.calculateTotalDuration(assignments, true)

        return WorkoutWithExercises(workout, assignments)
    }

    private fun getMovementTypeFromFocus(focus: String): MovementType {
        val f = focus.lowercase()
        return when {
            f.contains("chest") || f.contains("shoulder") || f.contains("tricep") || f.contains("push") -> MovementType.PUSH
            f.contains("back") || f.contains("bicep") || f.contains("pull") -> MovementType.PULL
            f.contains("leg") || f.contains("quad") || f.contains("hamstring") || f.contains("glute") || f.contains("calf") -> MovementType.LEGS
            f.contains("abs") || f.contains("core") || f.contains("oblique") -> MovementType.CORE
            f.contains("full") || f.contains("body") -> MovementType.FULL_BODY
            else -> MovementType.UNKNOWN
        }
    }

    private fun filterByFocusAndMovement(pool: List<Exercise>, focus: String, movement: MovementType): List<Exercise> {
        val keywords = focus.lowercase().split("and", "&", "/", ",").map { it.trim() }.filter { it.isNotEmpty() }
        
        // Priority 1: Direct Keyword Match
        val directMatches = pool.filter { ex ->
            val metadata = (ex.targetMuscle + " " + ex.bodyPart + " " + ex.target + " " + ex.name).lowercase()
            keywords.any { kw -> metadata.contains(kw) }
        }
        if (directMatches.isNotEmpty()) return directMatches

        // Priority 2: Movement Type Match (Ensures Chest focus doesn't get Hip warmups)
        if (movement != MovementType.UNKNOWN) {
            val movementMatches = pool.filter { getMovementType(it) == movement }
            if (movementMatches.isNotEmpty()) return movementMatches
        }

        // Priority 3: General/Universal exercises (Avoid specific unrelated body parts)
        return pool.filter { isUniversalWarmup(it) }
    }

    private fun isUniversalWarmup(ex: Exercise): Boolean {
        val name = ex.name.lowercase()
        val universalKeywords = listOf("jumping jack", "cardio", "full body", "burpee", "running", "march", "stretching", "treadmill")
        // Explicitly exclude unrelated areas if they are prominent in the name
        val avoidKeywords = listOf("hip", "groin", "leg swing") 
        return universalKeywords.any { name.contains(it) } && avoidKeywords.none { name.contains(it) }
    }

    private fun pickExercises(
        pool: List<Exercise>,
        count: Int,
        usedIds: Set<String>,
        random: Random
    ): List<Exercise> {
        if (pool.isEmpty()) return emptyList()

        val unused = pool.filter { !usedIds.contains(it.id) }.shuffled(random)
        val used = pool.filter { usedIds.contains(it.id) }.shuffled(random)

        return (unused + used).take(count)
    }

    private fun createAssignment(workoutId: Int, ex: Exercise, category: String, order: Int, difficulty: String, random: Random): WorkoutExercise {
        val isMain = category == "Exercise"

        val sets = when {
            !isMain -> 1
            difficulty == "Beginner" -> random.nextInt(3, 4)
            difficulty == "Intermediate" -> random.nextInt(3, 5)
            difficulty == "Advanced" -> random.nextInt(4, 6)
            else -> 3
        }

        val reps = when {
            !isMain -> "1"
            difficulty == "Beginner" -> "${random.nextInt(8, 11)}-${random.nextInt(12, 14)}"
            difficulty == "Intermediate" -> "${random.nextInt(10, 13)}-${random.nextInt(15, 17)}"
            difficulty == "Advanced" -> "${random.nextInt(12, 16)}-${random.nextInt(18, 21)}"
            else -> "10-12"
        }

        // Rest intervals constrained to 30-60s in increments of 5
        val rest = when {
            !isMain -> "0s"
            difficulty == "Beginner" -> "${(random.nextInt(10, 13) * 5)}s" // 50s, 55s, 60s
            difficulty == "Intermediate" -> "${(random.nextInt(8, 11) * 5)}s" // 40s, 45s, 50s
            difficulty == "Advanced" -> "${(random.nextInt(6, 9) * 5)}s" // 30s, 35s, 40s
            else -> "45s"
        }

        // Warmup/Cooldown constrained to 30-60s in increments of 5
        val duration = when {
            isMain -> ""
            category == "warmup" || category == "cooldown" -> "${(random.nextInt(6, 13) * 5)} sec"
            else -> "60 sec"
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
