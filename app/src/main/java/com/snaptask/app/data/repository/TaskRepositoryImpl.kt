package com.snaptask.app.data.repository

import android.content.Context
import com.snaptask.app.data.local.TaskDao
import com.snaptask.app.data.local.TaskEntity
import com.snaptask.app.domain.model.Task
import com.snaptask.app.reminder.TaskReminderScheduler
import com.snaptask.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepositoryImpl(
    private val context: Context,
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasksFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getPendingTasks(): Flow<List<Task>> {
        return taskDao.getPendingTasksFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasksFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getPinnedTasks(): Flow<List<Task>> {
        return taskDao.getPinnedTasksFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTaskById(id: Long): Task? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)?.toDomain()
    }

    override suspend fun getTopPendingTask(): Task? = withContext(Dispatchers.IO) {
        taskDao.getTopPendingTask()?.toDomain()
    }

    override suspend fun getPendingTasksForWidget(): List<Task> = withContext(Dispatchers.IO) {
        taskDao.getPendingTasksForWidget().map { it.toDomain() }
    }

    override suspend fun getProgressStats(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        Pair(taskDao.getCompletedCount(), taskDao.getTotalCount())
    }

    override suspend fun createTask(task: Task): Long = withContext(Dispatchers.IO) {
        val insertedId = taskDao.insertTask(TaskEntity.fromDomain(task))
        if (task.hasReminder) {
            TaskReminderScheduler.scheduleReminder(context, task.copy(id = insertedId))
        }
        insertedId
    }

    override suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.updateTask(TaskEntity.fromDomain(task))
        if (task.hasReminder && !task.isCompleted) {
            TaskReminderScheduler.scheduleReminder(context, task)
        } else {
            TaskReminderScheduler.cancelReminder(context, task.id)
        }
    }

    override suspend fun toggleTaskCompleted(id: Long, currentIsCompleted: Boolean) = withContext(Dispatchers.IO) {
        val next = !currentIsCompleted
        val completedAt = if (next) System.currentTimeMillis() else null
        taskDao.toggleCompleted(id, next, completedAt)
        if (next) {
            TaskReminderScheduler.cancelReminder(context, id)
        }
    }

    override suspend fun toggleTaskStatus(id: Long, currentStatus: Any?) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(id) ?: return@withContext
        val next = !task.isCompleted
        val completedAt = if (next) System.currentTimeMillis() else null
        taskDao.toggleCompleted(id, next, completedAt)
        if (next) {
            TaskReminderScheduler.cancelReminder(context, id)
        }
    }

    override suspend fun completeTaskWithAfterPhoto(id: Long, afterUri: String, caption: String) = withContext(Dispatchers.IO) {
        taskDao.completeTask(id, true, afterUri, caption, System.currentTimeMillis())
        TaskReminderScheduler.cancelReminder(context, id)
    }

    override suspend fun completeTaskWithProof(taskId: Long, afterImageUri: String?, completionCaption: String?, completedAt: Long) = withContext(Dispatchers.IO) {
        taskDao.completeTask(taskId, true, afterImageUri, completionCaption, completedAt)
        TaskReminderScheduler.cancelReminder(context, taskId)
    }

    override suspend fun togglePinToWidget(id: Long, currentIsPinned: Boolean) = withContext(Dispatchers.IO) {
        taskDao.togglePinned(id, !currentIsPinned)
    }

    override suspend fun togglePinTask(id: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        taskDao.togglePinned(id, isPinned)
    }

    override suspend fun renameFolderTasks(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        if (oldName.isNotBlank() && oldName != newName) {
            taskDao.renameFolderTasks(oldName, newName)
        }
    }

    override suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO) {
        FileUtils.deleteFile(task.beforeImageUri)
        FileUtils.deleteFile(task.afterImageUri)
        TaskReminderScheduler.cancelReminder(context, task.id)
        taskDao.deleteTask(TaskEntity.fromDomain(task))
    }
}
