package com.snaptask.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isPinnedToWidget DESC, createdAt DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY isPinnedToWidget DESC, createdAt DESC")
    fun getPendingTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isPinnedToWidget = 1 ORDER BY createdAt DESC")
    fun getPinnedTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY isPinnedToWidget DESC, createdAt DESC LIMIT 1")
    suspend fun getTopPendingTask(): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY isPinnedToWidget DESC, createdAt DESC LIMIT 5")
    suspend fun getPendingTasksForWidget(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND reminderTime IS NOT NULL AND reminderTime > :now AND reminderType != 'NONE' ORDER BY reminderTime ASC")
    suspend fun getPendingTasksWithFutureReminder(now: Long): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, afterImageUri = :afterUri, completionCaption = :caption, completedAt = :completedAt WHERE id = :id")
    suspend fun completeTask(id: Long, isCompleted: Boolean, afterUri: String?, caption: String?, completedAt: Long?)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun toggleCompleted(id: Long, isCompleted: Boolean, completedAt: Long?)

    @Query("UPDATE tasks SET isPinnedToWidget = :isPinned WHERE id = :id")
    suspend fun togglePinned(id: Long, isPinned: Boolean)

    @Query("UPDATE tasks SET isCompleted = CASE WHEN :newStatus = 'DONE' THEN 1 ELSE 0 END, completedAt = CASE WHEN :newStatus = 'DONE' THEN :timestamp ELSE NULL END WHERE id = :id")
    suspend fun updateTaskStatusWithTime(id: Long, newStatus: String, timestamp: Long)

    suspend fun updateTaskStatus(id: Long, newStatus: String) {
        updateTaskStatusWithTime(id, newStatus, System.currentTimeMillis())
    }

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}
