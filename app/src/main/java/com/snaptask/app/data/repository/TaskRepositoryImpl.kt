package com.snaptask.app.data.repository

import com.snaptask.app.data.local.Converters
import com.snaptask.app.data.local.TaskDao
import com.snaptask.app.data.local.TaskEntity
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskStatus
import com.snaptask.app.domain.model.TaskTag
import com.snaptask.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val converters: Converters = Converters()
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasksFlow().map { entities ->
            entities.map { it.toDomain(converters.toSubTaskList(it.subtasksJson)) }
        }
    }

    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        return taskDao.getTasksByStatusFlow(status.name).map { entities ->
            entities.map { it.toDomain(converters.toSubTaskList(it.subtasksJson)) }
        }
    }

    override fun getTasksByTag(tag: TaskTag): Flow<List<Task>> {
        return taskDao.getTasksByTagFlow(tag.name).map { entities ->
            entities.map { it.toDomain(converters.toSubTaskList(it.subtasksJson)) }
        }
    }

    override suspend fun getTaskById(id: Long): Task? = withContext(Dispatchers.IO) {
        val entity = taskDao.getTaskById(id) ?: return@withContext null
        entity.toDomain(converters.toSubTaskList(entity.subtasksJson))
    }

    override suspend fun getTopPendingTask(): Task? = withContext(Dispatchers.IO) {
        val entity = taskDao.getTopPendingTask() ?: return@withContext null
        entity.toDomain(converters.toSubTaskList(entity.subtasksJson))
    }

    override suspend fun getPendingTasksForWidget(): List<Task> = withContext(Dispatchers.IO) {
        taskDao.getPendingTasksForWidget().map {
            it.toDomain(converters.toSubTaskList(it.subtasksJson))
        }
    }

    override suspend fun createTask(task: Task): Long = withContext(Dispatchers.IO) {
        val json = converters.fromSubTaskList(task.subtasks)
        val entity = TaskEntity.fromDomain(task, json)
        taskDao.insertTask(entity)
    }

    override suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        val json = converters.fromSubTaskList(task.subtasks)
        val entity = TaskEntity.fromDomain(task, json)
        taskDao.updateTask(entity)
    }

    override suspend fun toggleTaskStatus(id: Long, currentStatus: TaskStatus) = withContext(Dispatchers.IO) {
        val next = currentStatus.next()
        taskDao.updateTaskStatus(id, next.name)
    }

    override suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO) {
        // Clean up image and thumbnail from disk
        FileUtils.deleteFile(task.imagePath)
        FileUtils.deleteFile(task.thumbnailPath)

        val json = converters.fromSubTaskList(task.subtasks)
        taskDao.deleteTask(TaskEntity.fromDomain(task, json))
    }
}
