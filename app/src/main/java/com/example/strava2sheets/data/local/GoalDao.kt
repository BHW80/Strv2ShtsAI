package com.example.strava2sheets.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun getAllGoalsFlow(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY id DESC")
    suspend fun getAllGoals(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: Long)
}
