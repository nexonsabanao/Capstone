package com.example.nutriority.data.repository

import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.data.local.WorkoutDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository for managing Workout and Exercise data.
 * It abstracts the data source (the WorkoutDao) from the rest of the app.
 * This class is the single source of truth for all workout-related data.
 */
class WorkoutRepository(private val workoutDao: WorkoutDao) {

    /**
     * A Flow that emits a list of all high-level workouts from the database, ordered by name.
     * The UI can collect this Flow to reactively update when the data changes.
     */
    val allWorkouts: Flow<List<Workout>> = workoutDao.getAllWorkouts()

    /**
     * A suspend function that returns a simple list of all workouts with their exercises.
     * This is useful for one-shot operations, like in the WorkoutPlanner.
     */
    suspend fun getAllWorkoutsList(): List<WorkoutWithExercises> {
        return workoutDao.getAllWorkoutsWithExercises().first()
    }

    /**
     * Retrieves a specific Workout along with its complete list of Exercises.
     * @param workoutId The ID of the workout to fetch.
     * @return A Flow that emits the specific WorkoutWithExercises object.
     */
    fun getWorkoutWithExercises(workoutId: Int): Flow<WorkoutWithExercises> {
        return workoutDao.getWorkoutWithExercises(workoutId)
    }

    /**
     * Retrieves a specific Workout by its ID.
     * @param workoutId The ID of the workout to fetch.
     * @return The Workout object.
     */
    suspend fun getWorkoutById(workoutId: Int): Workout? {
        return workoutDao.getWorkoutById(workoutId)
    }

    /**
     * Retrieves a specific Exercise by its ID.
     * @param exerciseId The ID of the exercise to fetch.
     * @return The Exercise object.
     */
    suspend fun getExerciseById(exerciseId: Int): Exercise? {
        return workoutDao.getExerciseById(exerciseId)
    }

    /**
     * A Flow that emits a list of all Workouts, each bundled with their respective Exercises.
     * This is useful for a screen that needs to show all workouts and a preview of their content.
     */
    val allWorkoutsWithExercises: Flow<List<WorkoutWithExercises>> = workoutDao.getAllWorkoutsWithExercises()

    /**
     * Inserts a new workout into the database.
     * This is a suspend function and must be called from a coroutine scope.
     * @param workout The workout object to insert.
     */
    suspend fun insertWorkout(workout: Workout) {
        workoutDao.insertWorkout(workout)
    }

    /**
     * Inserts a new exercise into the database.
     * This is a suspend function and must be called from a coroutine scope.
     * @param exercise The exercise object to insert.
     */
    suspend fun insertExercise(exercise: Exercise) {
        workoutDao.insertExercise(exercise)
    }

    suspend fun updateExercises(exercises: List<Exercise>) {
        workoutDao.updateExercises(exercises)
    }
}
