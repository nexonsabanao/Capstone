package com.example.nutriority.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nutriority.data.model.WorkoutLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WorkoutLog)

    @Query("SELECT * FROM workout_logs WHERE workoutId = :workoutId ORDER BY date DESC")
    fun getLogsForWorkout(workoutId: Int): Flow<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs")
    fun getWorkoutLogs(): Flow<List<WorkoutLog>>

}
