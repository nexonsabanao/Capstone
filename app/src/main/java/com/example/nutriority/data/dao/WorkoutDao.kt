package com.example.nutriority.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.nutriority.Models.Exercise
import com.example.nutriority.Models.Workout
import com.example.nutriority.Models.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // --- Insert Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)


    // --- Query Operations ---

    /**
     * Gets a list of all high-level workouts (e.g., Calisthenics, Full Body).
     */
    @Query("SELECT * FROM workouts ORDER BY name ASC")
    fun getAllWorkouts(): Flow<List<Workout>>

    /**
     * Gets a specific Workout with all its associated Exercises.
     * @Transaction ensures this is done as a single atomic operation.
     */
    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutWithExercises(workoutId: Int): Flow<WorkoutWithExercises>

    /**
     * Gets all Workouts, each bundled with its list of Exercises.
     */
    @Transaction
    @Query("SELECT * FROM workouts")
    fun getAllWorkoutsWithExercises(): Flow<List<WorkoutWithExercises>>
}
