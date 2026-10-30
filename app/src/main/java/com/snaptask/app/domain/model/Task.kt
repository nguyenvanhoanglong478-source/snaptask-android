package com.snaptask.app.domain.model

import com.snaptask.app.data.local.Converters

data class Task(
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
    val priority: String = TaskPriority.MEDIUM.name,
    val tag: String = "PERSONAL",
    val subTasksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val reminderTime: Long? = null,
    val reminderType: String = "NONE" // NONE, NOTIFICATION, ALARM
) {
    val hasImage: Boolean get() = beforeImageUri.isNotBlank()

    val priorityEnum: TaskPriority
        get() = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.MEDIUM)

    val parsedSubtasks: List<SubTask>
        get() = Converters().toSubTaskList(subTasksJson)

    val hasProof: Boolean
        get() = isCompleted && !afterImageUri.isNullOrBlank()

    val hasReminder: Boolean
        get() = reminderTime != null && reminderTime > System.currentTimeMillis() && !isCompleted
}
