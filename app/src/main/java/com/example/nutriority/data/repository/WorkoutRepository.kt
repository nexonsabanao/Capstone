package com.example.nutriority.data.repository

import com.example.nutriority.models.Exercise
import com.example.nutriority.models.Workout
import com.example.nutriority.models.WorkoutWithExercises
import com.example.nutriority.data.dao.WorkoutDao
import kotlinx.coroutines.flow.Flow

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
     * Retrieves a specific Workout along with its complete list of Exercises.
     * @param workoutId The ID of the workout to fetch.
     * @return A Flow that emits the specific WorkoutWithExercises object.
     */
    fun getWorkoutWithExercises(workoutId: Int): Flow<WorkoutWithExercises> {
        return workoutDao.getWorkoutWithExercises(workoutId)
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
}
