package com.chrona.ai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleTaskDao {
    @Insert
    suspend fun insert(task: ScheduleTask): Long

    @Insert
    suspend fun insertBehaviorEvent(event: TaskBehaviorEvent): Long

    @Query("SELECT * FROM schedule_tasks WHERE status = 'PENDING' ORDER BY COALESCE(startAt, createdAt) ASC")
    fun observeActiveTasks(): Flow<List<ScheduleTask>>

    @Query("SELECT * FROM task_behavior_events ORDER BY occurredAt DESC, id DESC")
    fun observeBehaviorEvents(): Flow<List<TaskBehaviorEvent>>

    @Query("UPDATE schedule_tasks SET status = 'DONE', updatedAt = :updatedAt WHERE id = :taskId AND status = 'PENDING'")
    suspend fun markDone(taskId: Long, updatedAt: Long): Int

    @Query("UPDATE schedule_tasks SET status = 'DELETED', updatedAt = :updatedAt WHERE id = :taskId AND status != 'DELETED'")
    suspend fun markDeleted(taskId: Long, updatedAt: Long): Int

    @Query("SELECT * FROM schedule_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: Long): ScheduleTask?
}
