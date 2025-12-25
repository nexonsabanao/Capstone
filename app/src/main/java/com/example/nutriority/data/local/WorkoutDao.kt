package com.example.nutriority.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // --- Insert Operations for Pre-population ---

    /**
     * Inserts a single workout and returns its new auto-generated ID.
     * This is crucial for linking exercises during pre-population.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long

    /**
     * Inserts a single exercise.
     * Note: For pre-population, insertAllExercises is more efficient.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)

    /**
     * Inserts a list of exercises.
     * Used in AppDatabase to efficiently insert all children after linking them to a parent workout.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExercises(exercises: List<Exercise>)


    // --- Query Operations ---

    /**
     * Returns the total number of workouts in the table.
     * Used in AppDatabase on startup to check if pre-population is needed.
     */
    // FIX: Corrected the table name by removing the trailing space.
    @Query("SELECT COUNT(id) FROM workouts")
    suspend fun getWorkoutCount(): Int

    /**
     * Gets a list of all high-level workouts (e.g., Calisthenics, Full Body).
     */
    @Query("SELECT * FROM workouts ORDER BY name ASC")
    fun getAllWorkouts(): Flow<List<Workout>>

    /**
     * Gets a specific Workout by its ID.
     */
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Int): Workout?

    /**
     * Gets a specific Exercise by its ID.
     */
    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: Int): Exercise?

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

    @Update
    suspend fun updateExercises(exercises: List<Exercise>)
}