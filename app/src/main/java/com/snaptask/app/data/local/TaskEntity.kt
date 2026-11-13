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
    val beforeImageUri: String = "",
    val afterImageUri: String? = null,
    val completionCaption: String? = null,
    val isCompleted: Boolean = false,
    val isPinnedToWidget: Boolean = false,
    val isFavorite: Boolean = false,
    val folderName: String = "Cá nhân",
    val priority: String = "MEDIUM",
    val tag: String = "PERSONAL",
    val subTasksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val reminderTime: Long? = null,
    val reminderType: String = "NONE"
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
            isFavorite = isFavorite,
            folderName = folderName,
            priority = priority,
            tag = tag,
            subTasksJson = subTasksJson,
            createdAt = createdAt,
            dueDate = dueDate,
            completedAt = completedAt,
            reminderTime = reminderTime,
            reminderType = reminderType
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
                isFavorite = task.isFavorite,
                folderName = task.folderName,
                priority = task.priority,
                tag = task.tag,
                subTasksJson = task.subTasksJson,
                createdAt = task.createdAt,
                dueDate = task.dueDate,
                completedAt = task.completedAt,
                reminderTime = task.reminderTime,
                reminderType = task.reminderType
            )
        }
    }
}
