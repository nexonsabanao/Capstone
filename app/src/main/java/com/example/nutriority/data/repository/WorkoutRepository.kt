package com.example.nutriority.data.repository

import android.app.Application
import com.example.nutriority.data.local.WorkoutDao
import com.example.nutriority.data.local.WorkoutLogDao
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.data.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val workoutLogDao: WorkoutLogDao,
    private val application: Application
) {

    val allWorkouts: Flow<List<Workout>> = workoutDao.getAllWorkouts().map { workouts ->
        workouts.map { workout ->
            workout.apply {
                imageResId = application.resources.getIdentifier(imageName, "drawable", application.packageName)
            }
        }
    }

    suspend fun getAllWorkoutsList(): List<WorkoutWithExercises> {
        return workoutDao.getAllWorkoutsWithExercises().first().map { it.applyImages() }
    }

    // FIX: Ensure all exercises (warmup, main, cooldown) are included
    fun getWorkoutWithExercises(workoutId: Int): Flow<WorkoutWithExercises> {
        return workoutDao.getWorkoutWithExercises(workoutId).map { it.applyImages() }
    }

    suspend fun getWorkoutById(workoutId: Int): Workout? {
        return workoutDao.getWorkoutById(workoutId)
    }

    suspend fun getExerciseById(exerciseId: Int): Exercise? {
        return workoutDao.getExerciseById(exerciseId)
    }

    val allWorkoutsWithExercises: Flow<List<WorkoutWithExercises>> = 
        workoutDao.getAllWorkoutsWithExercises().map { list -> list.map { it.applyImages() } }

    suspend fun insertWorkout(workout: Workout) {
        workoutDao.insertWorkout(workout)
    }

    suspend fun insertExercise(exercise: Exercise) {
        workoutDao.insertExercise(exercise)
    }

    suspend fun updateExercise(exercise: Exercise) {
        workoutDao.updateExercise(exercise)
    }

    suspend fun updateExercises(exercises: List<Exercise>) {
        workoutDao.updateExercises(exercises)
    }

    suspend fun updateWorkout(workout: Workout) {
        workoutDao.updateWorkout(workout)
    }

    suspend fun unlinkExercisesFromWorkout(workoutId: Int) {
        workoutDao.unlinkExercisesFromWorkout(workoutId)
    }

    suspend fun insertWorkoutLog(log: WorkoutLog) {
        workoutLogDao.insertLog(log)
    }

    fun getLogsForWorkout(workoutId: Int): Flow<List<WorkoutLog>> {
        return workoutLogDao.getLogsForWorkout(workoutId)
    }

    fun getWorkoutLogs(): Flow<List<WorkoutLog>> {
        return workoutLogDao.getWorkoutLogs()
    }

    fun getAllExercises(): Flow<List<Exercise>> {
        return workoutDao.getAllExercises().map { exercises ->
            exercises.map { exercise ->
                exercise.apply {
                    imageResId = application.resources.getIdentifier(imageName, "drawable", application.packageName)
                }
            }
        }
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

    // Helper to attach images to all exercises in a workout
    private fun WorkoutWithExercises.applyImages(): WorkoutWithExercises {
        this.exercises.forEach {
            it.imageResId = application.resources.getIdentifier(it.imageName, "drawable", application.packageName)
        }
        return this
    }
}
