package com.snaptask.app.domain.model

data class Task(
    val id: Long = 0,
    val title: String,
    val imagePath: String,
    val thumbnailPath: String,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val tag: TaskTag = TaskTag.PERSONAL,
    val subtasks: List<SubTask> = emptyList(),
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    val isCompleted: Boolean
        get() = status == TaskStatus.DONE

    val completedSubtasksCount: Int
        get() = subtasks.count { it.isDone }
}
