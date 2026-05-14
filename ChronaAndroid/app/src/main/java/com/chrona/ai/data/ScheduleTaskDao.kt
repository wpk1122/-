package com.chrona.ai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleTaskDao {
    @Insert
    suspend fun insert(task: ScheduleTask): Long

    @Query("SELECT * FROM schedule_tasks WHERE status != 'DELETED' ORDER BY COALESCE(startAt, createdAt) ASC")
    fun observeActiveTasks(): Flow<List<ScheduleTask>>

    @Query("UPDATE schedule_tasks SET status = :status, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateStatus(taskId: Long, status: TaskStatus, updatedAt: Long)

    @Query("SELECT * FROM schedule_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: Long): ScheduleTask?
}
