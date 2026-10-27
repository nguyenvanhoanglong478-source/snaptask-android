package com.snaptask.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.snaptask.app.domain.model.Task

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val beforeImageUri: String,
    val afterImageUri: String? = null,
    val completionCaption: String? = null,
    val isCompleted: Boolean = false,
    val isPinnedToWidget: Boolean = false,
    val priority: String = "MEDIUM",
    val tag: String = "PERSONAL",
    val subTasksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val completedAt: Long? = null
) {
    fun toDomain(): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            beforeImageUri = beforeImageUri,
            afterImageUri = afterImageUri,
            completionCaption = completionCaption,
            isCompleted = isCompleted,
            isPinnedToWidget = isPinnedToWidget,
            priority = priority,
            tag = tag,
            subTasksJson = subTasksJson,
            createdAt = createdAt,
            dueDate = dueDate,
            completedAt = completedAt
        )
    }

    companion object {
        fun fromDomain(task: Task): TaskEntity {
            return TaskEntity(
                id = task.id,
                title = task.title,
                description = task.description,
                beforeImageUri = task.beforeImageUri,
                afterImageUri = task.afterImageUri,
                completionCaption = task.completionCaption,
                isCompleted = task.isCompleted,
                isPinnedToWidget = task.isPinnedToWidget,
                priority = task.priority,
                tag = task.tag,
                subTasksJson = task.subTasksJson,
                createdAt = task.createdAt,
                dueDate = task.dueDate,
                completedAt = task.completedAt
            )
        }
    }
}
