package com.example.nutriority.data.repository

import android.app.Application
import com.example.nutriority.data.local.WorkoutDao
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WorkoutRepository(private val workoutDao: WorkoutDao, private val application: Application) {

    val allWorkouts: Flow<List<Workout>> = workoutDao.getAllWorkouts().map { workouts ->
        workouts.map {
            it.apply {
                val resources = application.resources
                val packageName = application.packageName
                imageResId = resources.getIdentifier(it.imageName, "drawable", packageName)
            }
        }
    }

    suspend fun getAllWorkoutsList(): List<WorkoutWithExercises> {
        return workoutDao.getAllWorkoutsWithExercises().first()
    }

    fun getWorkoutWithExercises(workoutId: Int): Flow<WorkoutWithExercises> {
        return workoutDao.getWorkoutWithExercises(workoutId)
    }

    suspend fun getWorkoutById(workoutId: Int): Workout? {
        return workoutDao.getWorkoutById(workoutId)
    }

    suspend fun getExerciseById(exerciseId: Int): Exercise? {
        return workoutDao.getExerciseById(exerciseId)
    }

    val allWorkoutsWithExercises: Flow<List<WorkoutWithExercises>> = workoutDao.getAllWorkoutsWithExercises()

    suspend fun insertWorkout(workout: Workout) {
        workoutDao.insertWorkout(workout)
    }

    suspend fun insertExercise(exercise: Exercise) {
        workoutDao.insertExercise(exercise)
    }

    suspend fun updateExercises(exercises: List<Exercise>) {
        workoutDao.updateExercises(exercises)
    }

    fun getAllExercises(): Flow<List<Exercise>> {
        return workoutDao.getAllExercises()
    }

    fun getUniqueTargetMuscles(): Flow<List<String>> {
        return workoutDao.getUniqueTargetMuscles().map { muscleList ->
            muscleList
                .flatMap { it.split(',') }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
    }
}
