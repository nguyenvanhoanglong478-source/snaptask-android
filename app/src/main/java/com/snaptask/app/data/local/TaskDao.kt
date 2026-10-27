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

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority DESC, createdAt DESC")
    fun getTasksByStatusFlow(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE tag = :tag ORDER BY createdAt DESC")
    fun getTasksByTagFlow(tag: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status != 'DONE' ORDER BY priority DESC, createdAt DESC LIMIT 1")
    suspend fun getTopPendingTask(): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status != 'DONE' ORDER BY priority DESC, createdAt DESC LIMIT 5")
    suspend fun getPendingTasksForWidget(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :newStatus WHERE id = :id")
    suspend fun updateTaskStatus(id: Long, newStatus: String)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}
