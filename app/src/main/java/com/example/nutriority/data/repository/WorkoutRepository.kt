package com.example.nutriority.data.repository

import android.app.Application
import com.example.nutriority.data.local.WorkoutDao
import com.example.nutriority.data.local.WorkoutLogDao
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutLog
import com.example.nutriority.data.model.WorkoutSessionLog
import com.example.nutriority.data.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow
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
        return workoutDao.getAllWorkouts().first().map { workout ->
            workoutDao.getWorkoutWithExercises(workout.id).first().applyImages()
        }
    }

    fun getWorkoutWithExercises(workoutId: Int): Flow<WorkoutWithExercises> {
        return workoutDao.getWorkoutWithExercises(workoutId).map { it.applyImages() }
    }

    suspend fun getWorkoutById(workoutId: Int): Workout? {
        return workoutDao.getWorkoutById(workoutId)
    }

    suspend fun getExerciseById(exerciseId: String): Exercise? {
        return workoutDao.getExerciseById(exerciseId)
    }

    val allWorkoutsWithExercises: Flow<List<WorkoutWithExercises>> = 
        workoutDao.getAllWorkouts().map { list -> 
            list.map { workout ->
                workoutDao.getWorkoutWithExercises(workout.id).first().applyImages()
            }
        }

    suspend fun insertWorkout(workout: Workout) {
        workoutDao.insertWorkout(workout)
    }

    suspend fun insertExercise(exercise: Exercise) {
        workoutDao.insertExercise(exercise)
    }

    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise) {
        workoutDao.insertWorkoutExercise(workoutExercise)
    }

    suspend fun updateExercise(exercise: Exercise) {
        workoutDao.updateExercise(exercise)
    }

    suspend fun updateWorkout(workout: Workout) {
        workoutDao.updateWorkout(workout)
    }

    suspend fun updateExerciseCompletion(workoutId: Int, exerciseId: String, category: String, completed: Boolean) {
        workoutDao.updateExerciseCompletion(workoutId, exerciseId, category, completed)
    }

    suspend fun updateWorkoutWithExercises(workout: Workout, workoutExercises: List<WorkoutExercise>) {
        workoutDao.updateWorkoutWithExercises(workout, workoutExercises)
    }

    suspend fun insertWorkoutLog(log: WorkoutLog) {
        workoutLogDao.insertLog(log)
    }

    suspend fun insertSessionLog(log: WorkoutSessionLog) {
        workoutDao.insertSessionLog(log)
    }

    fun getLatestSessionLog(): Flow<WorkoutSessionLog?> {
        return workoutDao.getLatestSessionLog()
    }

    fun getAllSessionLogs(): Flow<List<WorkoutSessionLog>> {
        return workoutDao.getAllSessionLogs()
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
        return workoutDao.getAllExercises().map { exercises ->
            exercises
                .flatMap { it.targetMuscle.split(',') }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
    }

    private fun WorkoutWithExercises.applyImages(): WorkoutWithExercises {
        this.exerciseAssignments.forEach { assignment ->
            assignment.exercise.imageResId = application.resources.getIdentifier(
                assignment.exercise.imageName, 
                "drawable", 
                application.packageName
            )
        }
        return this
    }
}
