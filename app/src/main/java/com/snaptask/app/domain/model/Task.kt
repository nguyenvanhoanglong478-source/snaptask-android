package com.snaptask.app.domain.model

import com.snaptask.app.data.local.Converters
import com.snaptask.app.util.DateTimeUtils

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val beforeImageUri: String,
    val afterImageUri: String? = null,
    val completionCaption: String? = null,
    val isCompleted: Boolean = false,
    val isPinnedToWidget: Boolean = false,
    val priority: String = TaskPriority.MEDIUM.name,
    val tag: String = TaskTag.PERSONAL.name,
    val subTasksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val reminderTime: Long? = null,
    val reminderType: String = "NONE" // NONE, NOTIFICATION, ALARM
) {
    // Backwards Compatibility Accessors
    val imagePath: String get() = beforeImageUri
    val thumbnailPath: String get() = beforeImageUri
    val status: TaskStatus get() = if (isCompleted) TaskStatus.DONE else TaskStatus.TODO

    val priorityEnum: TaskPriority
        get() = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.MEDIUM)

    val tagEnum: TaskTag
        get() = runCatching { TaskTag.valueOf(tag) }.getOrDefault(TaskTag.PERSONAL)

    val parsedSubtasks: List<SubTask>
        get() = Converters().toSubTaskList(subTasksJson)

    val subtasks: List<SubTask>
        get() = parsedSubtasks

    val completedSubtasksCount: Int
        get() = parsedSubtasks.count { it.isDone }

    val progressPercent: Int
        get() {
            val list = parsedSubtasks
            if (list.isEmpty()) return if (isCompleted) 100 else 0
            return (list.count { it.isDone } * 100) / list.size
        }

    val durationFormatted: String
        get() = DateTimeUtils.formatTaskDuration(createdAt, completedAt)

    val createdAtTimeFormatted: String
        get() = DateTimeUtils.formatTime(createdAt)

    val hasProof: Boolean
        get() = isCompleted && !afterImageUri.isNullOrBlank()

    val hasReminder: Boolean
        get() = reminderTime != null && reminderTime > System.currentTimeMillis() && !isCompleted

    val reminderFormatted: String?
        get() = reminderTime?.let { DateTimeUtils.formatDateTime(it) }
}
