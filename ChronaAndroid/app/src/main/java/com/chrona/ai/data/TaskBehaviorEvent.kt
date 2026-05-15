package com.chrona.ai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_behavior_events")
data class TaskBehaviorEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long?,
    val type: String,
    val occurredAt: Long,
    val metadata: String? = null
)

enum class BehaviorEventType {
    CREATED,
    COMPLETED,
    DELETED
}
