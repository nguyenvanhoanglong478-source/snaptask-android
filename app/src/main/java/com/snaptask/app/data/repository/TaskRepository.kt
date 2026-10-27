package com.snaptask.app.data.repository

import com.snaptask.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getPendingTasks(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>
    fun getPinnedTasks(): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Task?
    suspend fun getTopPendingTask(): Task?
    suspend fun getPendingTasksForWidget(): List<Task>
    suspend fun getProgressStats(): Pair<Int, Int> // Completed, Total
    suspend fun createTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun toggleTaskCompleted(id: Long, currentIsCompleted: Boolean)
    suspend fun toggleTaskStatus(id: Long, currentStatus: Any? = null)
    suspend fun completeTaskWithAfterPhoto(id: Long, afterUri: String, caption: String)
    suspend fun completeTaskWithProof(taskId: Long, afterImageUri: String?, completionCaption: String?, completedAt: Long = System.currentTimeMillis())
    suspend fun togglePinToWidget(id: Long, currentIsPinned: Boolean)
    suspend fun togglePinTask(id: Long, isPinned: Boolean)
    suspend fun deleteTask(task: Task)
}
