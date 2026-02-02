package com.example.nutriority.planner

import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutGenerator @Inject constructor() {

    enum class MovementType { PUSH, PULL, LEGS, CORE, UNKNOWN }
    enum class ExerciseRole { COMPOUND, ISOLATION, FINISHER_CORE, WARMUP, COOLDOWN }

    fun generateStructuredWorkout(
        movementType: MovementType,
        difficulty: String,
        allExercises: List<Exercise>
    ): WorkoutWithExercises {
        val filtered = allExercises.filter { getMovementType(it) == movementType }
        
        val compoundPool = filtered.filter { getExerciseRole(it) == ExerciseRole.COMPOUND }
        val isolationPool = filtered.filter { getExerciseRole(it) == ExerciseRole.ISOLATION }
        
        // For Core/Finisher, we pull from the CORE movement type
        val finisherPool = allExercises.filter { getMovementType(it) == MovementType.CORE && getExerciseRole(it) == ExerciseRole.FINISHER_CORE }

        // Pull Warmup and Cooldown exercises
        val warmupPool = allExercises.filter { getExerciseRole(it) == ExerciseRole.WARMUP }
        val cooldownPool = allExercises.filter { getExerciseRole(it) == ExerciseRole.COOLDOWN }

        val selectedWarmup = warmupPool.shuffled().take(2)
        val selectedCompound = compoundPool.shuffled().take(2)
        val selectedIsolation = isolationPool.shuffled().take(2)
        val selectedFinisher = finisherPool.shuffled().take(1)
        val selectedCooldown = cooldownPool.shuffled().take(2)

        val workoutId = -(movementType.ordinal * 100 + difficulty.hashCode()).coerceAtLeast(1)
        val workoutName = when(movementType) {
            MovementType.PUSH -> "Push Day"
            MovementType.PULL -> "Pull Day"
            MovementType.LEGS -> "Leg Day"
            MovementType.CORE -> "Core Day"
            else -> "Workout"
        }

        val workout = Workout(
            id = workoutId,
            name = workoutName,
            description = "Complete structured session: Warmup -> Main Workout -> Cooldown.",
            category = "Strength",
            targetMuscle = movementType.name,
            difficulty = difficulty,
            includeWarmupCooldown = true
        )

        val assignments = mutableListOf<WorkoutExercise>()
        var order = 0

        // 1. Warmup (Dynamic movements)
        selectedWarmup.forEach { assignments.add(createAssignment(workoutId, it, ExerciseRole.WARMUP, order++)) }

        // 2. Main Workout: 2 Compound -> 2 Isolation -> 1 Finisher
        selectedCompound.forEach { assignments.add(createAssignment(workoutId, it, ExerciseRole.COMPOUND, order++)) }
        selectedIsolation.forEach { assignments.add(createAssignment(workoutId, it, ExerciseRole.ISOLATION, order++)) }
        selectedFinisher.forEach { assignments.add(createAssignment(workoutId, it, ExerciseRole.FINISHER_CORE, order++)) }

        // 3. Cooldown (Stretching)
        selectedCooldown.forEach { assignments.add(createAssignment(workoutId, it, ExerciseRole.COOLDOWN, order++)) }

        val details = assignments.map { assignment ->
            val exercise = allExercises.find { it.id == assignment.exerciseId }!!
            WorkoutExerciseWithDetail(assignment, exercise)
        }

        return WorkoutWithExercises(workout, details)
    }

    private fun createAssignment(workoutId: Int, exercise: Exercise, role: ExerciseRole, order: Int): WorkoutExercise {
        val sets: Int
        val reps: String
        val rest: String
        var duration = ""

        when (role) {
            ExerciseRole.WARMUP -> {
                sets = 1
                reps = "1"
                rest = "0s"
                duration = "1 min"
            }
            ExerciseRole.COMPOUND -> {
                sets = 4
                reps = "10"
                rest = "90s"
            }
            ExerciseRole.ISOLATION -> {
                sets = 3
                reps = "12"
                rest = "60s"
            }
            ExerciseRole.FINISHER_CORE -> {
                sets = 2
                reps = "20"
                rest = "45s"
            }
            ExerciseRole.COOLDOWN -> {
                sets = 1
                reps = "1"
                rest = "0s"
                duration = "30s"
            }
        }

        return WorkoutExercise(workoutId, exercise.id, if (role == ExerciseRole.WARMUP) "Warmup" else if (role == ExerciseRole.COOLDOWN) "Cooldown" else "Exercise", sets, reps, rest, duration, order)
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
        
        // 1. Explicit categories from Firestore
        if (category.contains("warmup")) return ExerciseRole.WARMUP
        if (category.contains("cooldown") || category.contains("stretch")) return ExerciseRole.COOLDOWN

        // 2. Keyword detection for Warmups
        val warmupKeywords = listOf("jumping jack", "arm circle", "leg swing", "high knee")
        if (warmupKeywords.any { name.contains(it) }) return ExerciseRole.WARMUP

        // 3. Keyword detection for Cooldowns
        val cooldownKeywords = listOf("cat-cow", "child's pose", "seated forward fold", "stretch")
        if (cooldownKeywords.any { name.contains(it) }) return ExerciseRole.COOLDOWN

        // 4. Main Workout roles
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
