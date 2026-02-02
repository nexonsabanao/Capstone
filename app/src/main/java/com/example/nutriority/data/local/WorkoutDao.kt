package com.example.nutriority.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutSessionLog
import com.example.nutriority.data.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExercises(exercises: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise)

    @Update
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionLog(log: WorkoutSessionLog): Long

    @Query("SELECT * FROM workout_session_logs ORDER BY date DESC LIMIT 1")
    fun getLatestSessionLog(): Flow<WorkoutSessionLog?>

    @Query("SELECT * FROM workout_session_logs ORDER BY date DESC")
    fun getAllSessionLogs(): Flow<List<WorkoutSessionLog>>

    @Query("SELECT COUNT(id) FROM workouts")
    suspend fun getWorkoutCount(): Int

    @Query("SELECT COUNT(id) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Query("SELECT * FROM workouts ORDER BY name ASC")
    fun getAllWorkouts(): Flow<List<Workout>>

    @Transaction
    @Query("SELECT * FROM workouts")
    fun getAllWorkoutsWithExercises(): Flow<List<WorkoutWithExercises>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: String): Exercise?

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Int): Workout?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutWithExercises(workoutId: Int): Flow<WorkoutWithExercises?>

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Query("UPDATE workout_exercises SET isCompleted = :completed WHERE workoutId = :workoutId AND exerciseId = :exerciseId AND category = :category")
    suspend fun updateExerciseCompletion(workoutId: Int, exerciseId: String, category: String, completed: Boolean)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutExercises(workoutId: Int)

    @Delete
    suspend fun deleteWorkout(workout: Workout)

    @Query("DELETE FROM workout_session_logs")
    suspend fun deleteAllSessionLogs()

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()

    @Transaction
    suspend fun updateWorkoutWithExercises(workout: Workout, workoutExercises: List<WorkoutExercise>) {
        insertWorkout(workout)
        deleteWorkoutExercises(workout.id)
        workoutExercises.forEach { insertWorkoutExercise(it) }
    }

    @Transaction
    suspend fun deleteFullWorkout(workout: Workout) {
        deleteWorkoutExercises(workout.id)
        deleteWorkout(workout)
    }

    @Transaction
    suspend fun deleteAllCustomWorkouts() {
        val customWorkouts = getWorkoutsWithIdGreaterThan(25)
        customWorkouts.forEach { workout ->
            deleteWorkoutExercises(workout.id)
            deleteWorkout(workout)
        }
    }

    @Query("SELECT * FROM workouts WHERE id > :id")
    suspend fun getWorkoutsWithIdGreaterThan(id: Int): List<Workout>

    @Query("SELECT * FROM exercises WHERE (bodyPart LIKE '%' || :focus || '%' OR target LIKE '%' || :focus || '%') AND difficulty = :difficulty AND category NOT LIKE '%warmup%' AND category NOT LIKE '%cooldown%'")
    suspend fun getExercisesByFocusAndDifficulty(focus: String, difficulty: String): List<Exercise>

    @Query("SELECT * FROM exercises WHERE (bodyPart LIKE '%' || :focus || '%' OR target LIKE '%' || :focus || '%') AND category NOT LIKE '%warmup%' AND category NOT LIKE '%cooldown%'")
    suspend fun getExercisesByFocus(focus: String): List<Exercise>

    @Query("SELECT * FROM exercises WHERE category LIKE '%' || :category || '%'")
    suspend fun getExercisesByCategory(category: String): List<Exercise>
}