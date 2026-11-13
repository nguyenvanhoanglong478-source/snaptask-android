package com.snaptask.app.data.repository

import com.snaptask.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    suspend fun createTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun toggleTaskStatus(id: Long)
    suspend fun completeTaskWithProof(taskId: Long, afterImageUri: String?, completionCaption: String?, completedAt: Long = System.currentTimeMillis())
    suspend fun renameFolderTasks(oldName: String, newName: String)
    suspend fun deleteTask(task: Task)
}
