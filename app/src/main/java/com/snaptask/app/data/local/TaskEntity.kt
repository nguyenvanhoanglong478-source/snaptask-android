package com.snaptask.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.snaptask.app.domain.model.SubTask
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import com.snaptask.app.domain.model.TaskStatus
import com.snaptask.app.domain.model.TaskTag

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val imagePath: String,
    val thumbnailPath: String,
    val status: String,
    val priority: Int,
    val tag: String,
    val subtasksJson: String,
    val dueDate: Long?,
    val createdAt: Long,
    val isSynced: Boolean
) {
    fun toDomain(subtasks: List<SubTask>): Task {
        return Task(
            id = id,
            title = title,
            imagePath = imagePath,
            thumbnailPath = thumbnailPath,
            status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.TODO),
            priority = TaskPriority.fromLevel(priority),
            tag = runCatching { TaskTag.valueOf(tag) }.getOrDefault(TaskTag.PERSONAL),
            subtasks = subtasks,
            dueDate = dueDate,
            createdAt = createdAt,
            isSynced = isSynced
        )
    }

    companion object {
        fun fromDomain(task: Task, subtasksJson: String): TaskEntity {
            return TaskEntity(
                id = task.id,
                title = task.title,
                imagePath = task.imagePath,
                thumbnailPath = task.thumbnailPath,
                status = task.status.name,
                priority = task.priority.level,
                tag = task.tag.name,
                subtasksJson = subtasksJson,
                dueDate = task.dueDate,
                createdAt = task.createdAt,
                isSynced = task.isSynced
            )
        }
    }
}
