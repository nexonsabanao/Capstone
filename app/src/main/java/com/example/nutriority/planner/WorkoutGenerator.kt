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

    enum class MovementType { PUSH, PULL, LEGS, CORE, FULL_BODY, UNKNOWN }
    enum class ExerciseRole { COMPOUND, ISOLATION, FINISHER_CORE, WARMUP, COOLDOWN }

    // Removed the workout generation logic as requested.
    // This class is kept for its utility enums and helper methods used by other components.

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

        val warmupKeywords = listOf("jumping jack", "arm circle", "leg swing", "high knee")
        if (warmupKeywords.any { name.contains(it) }) return ExerciseRole.WARMUP

        val cooldownKeywords = listOf("cat-cow", "child's pose", "seated forward fold", "stretch")
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
