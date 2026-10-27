package com.snaptask.app.data.repository

import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskStatus
import com.snaptask.app.domain.model.TaskTag
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>
    fun getTasksByTag(tag: TaskTag): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Task?
    suspend fun getTopPendingTask(): Task?
    suspend fun getPendingTasksForWidget(): List<Task>
    suspend fun createTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun toggleTaskStatus(id: Long, currentStatus: TaskStatus)
    suspend fun deleteTask(task: Task)
}
