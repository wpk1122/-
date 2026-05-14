package com.chrona.ai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_tasks")
data class ScheduleTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String?,
    val startAt: Long?,
    val endAt: Long?,
    val status: TaskStatus,
    val sourceText: String,
    val createdAt: Long,
    val updatedAt: Long
)

enum class TaskStatus {
    PENDING,
    DONE,
    DELETED
}
