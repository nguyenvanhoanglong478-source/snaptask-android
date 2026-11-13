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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TaskRepositoryImpl internal constructor(
    private val taskDao: TaskDao,
    private val scheduleReminder: (Task) -> Unit = {},
    private val cancelReminder: (Long) -> Unit = {},
    private val deleteImages: (Array<out String?>) -> Unit = { paths -> FileUtils.deleteTaskImages(*paths) },
    private val onTasksChanged: () -> Unit = {},
    private val onTaskDeleted: (Long) -> Unit = {}
) : TaskRepository {

    constructor(
        context: Context,
        taskDao: TaskDao,
        onTasksChanged: () -> Unit = {},
        onTaskDeleted: (Long) -> Unit = {}
    ) : this(
        taskDao = taskDao,
        scheduleReminder = { task -> TaskReminderScheduler.scheduleReminder(context, task) },
        cancelReminder = { taskId -> TaskReminderScheduler.cancelReminder(context, taskId) },
        deleteImages = { paths -> FileUtils.deleteTaskImages(*paths) },
        onTasksChanged = onTasksChanged,
        onTaskDeleted = onTaskDeleted
    )

    private val writeMutex = Mutex()

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasksFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createTask(task: Task): Long = withContext(Dispatchers.IO) {
        val insertedId = writeMutex.withLock {
            val id = taskDao.insertTask(TaskEntity.fromDomain(task))
            if (task.hasReminder) {
                scheduleReminder(task.copy(id = id))
            }
            id
        }
        onTasksChanged()
        insertedId
    }

    override suspend fun updateTask(task: Task): Unit = withContext(Dispatchers.IO) {
        val oldPathsToDelete = writeMutex.withLock {
            val existing = taskDao.getTaskById(task.id)
            taskDao.updateTask(TaskEntity.fromDomain(task))
            val replaced = if (existing != null) {
                buildList {
                    val oldBefore = existing.beforeImageUri
                    if (oldBefore.isNotBlank() && oldBefore != task.beforeImageUri && oldBefore != task.afterImageUri) {
                        add(oldBefore)
                    }
                    val oldAfter = existing.afterImageUri
                    if (!oldAfter.isNullOrBlank() && oldAfter != task.afterImageUri && oldAfter != task.beforeImageUri) {
                        add(oldAfter)
                    }
                }
            } else {
                emptyList()
            }
            if (task.hasReminder && !task.isCompleted) {
                scheduleReminder(task)
            } else {
                cancelReminder(task.id)
            }
            replaced
        }
        if (oldPathsToDelete.isNotEmpty()) {
            deleteImages(oldPathsToDelete.toTypedArray())
        }
        onTasksChanged()
    }

    override suspend fun toggleTaskStatus(id: Long): Unit = withContext(Dispatchers.IO) {
        val changed = writeMutex.withLock {
            val task = taskDao.getTaskById(id) ?: return@withLock false
            val next = !task.isCompleted
            val completedAt = if (next) System.currentTimeMillis() else null
            taskDao.toggleCompleted(id, next, completedAt)
            if (next) {
                cancelReminder(id)
            } else {
                val reopened = task.toDomain().copy(isCompleted = false, completedAt = null)
                if (reopened.hasReminder) {
                    scheduleReminder(reopened)
                }
            }
            true
        }
        if (changed) {
            onTasksChanged()
        }
    }

    override suspend fun completeTaskWithProof(
        taskId: Long,
        afterImageUri: String?,
        completionCaption: String?,
        completedAt: Long
    ): Unit = withContext(Dispatchers.IO) {
        var oldAfterToDelete: String? = null
        writeMutex.withLock {
            val existing = taskDao.getTaskById(taskId)
            taskDao.completeTask(taskId, true, afterImageUri, completionCaption, completedAt)
            val oldAfter = existing?.afterImageUri
            if (!oldAfter.isNullOrBlank() && oldAfter != afterImageUri && oldAfter != existing.beforeImageUri) {
                oldAfterToDelete = oldAfter
            }
            cancelReminder(taskId)
        }
        val toDelete = oldAfterToDelete
        if (toDelete != null) {
            deleteImages(arrayOf(toDelete))
        }
        onTasksChanged()
    }

    override suspend fun renameFolderTasks(oldName: String, newName: String): Unit = withContext(Dispatchers.IO) {
        if (oldName.isNotBlank() && newName.isNotBlank() && oldName != newName) {
            writeMutex.withLock {
                taskDao.renameFolderTasks(oldName, newName)
            }
            onTasksChanged()
        }
    }

    override suspend fun deleteTask(task: Task): Unit = withContext(Dispatchers.IO) {
        val pathsToDelete = writeMutex.withLock {
            val existing = taskDao.getTaskById(task.id)
            taskDao.deleteTask(TaskEntity.fromDomain(task))
            cancelReminder(task.id)
            listOfNotNull(
                task.beforeImageUri.takeIf { it.isNotBlank() },
                task.afterImageUri?.takeIf { it.isNotBlank() },
                existing?.beforeImageUri?.takeIf { it.isNotBlank() },
                existing?.afterImageUri?.takeIf { it.isNotBlank() }
            ).distinct()
        }
        if (pathsToDelete.isNotEmpty()) {
            deleteImages(pathsToDelete.toTypedArray())
        }
        onTaskDeleted(task.id)
        onTasksChanged()
    }
}
