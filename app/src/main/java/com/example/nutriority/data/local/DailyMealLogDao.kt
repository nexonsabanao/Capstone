package com.example.nutriority.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nutriority.data.model.DailyMealLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyMealLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyMealLog)

    @Query("SELECT * FROM daily_meal_logs WHERE date >= :startOfDay AND date < :endOfDay ORDER BY date DESC")
    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DailyMealLog>>

    @Query("SELECT * FROM daily_meal_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<DailyMealLog>>

    @Query("DELETE FROM daily_meal_logs WHERE id = :logId")
    suspend fun deleteLog(logId: Int)
}
