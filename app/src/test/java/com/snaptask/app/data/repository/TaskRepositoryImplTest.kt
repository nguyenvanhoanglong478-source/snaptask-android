package com.snaptask.app.data.repository

import com.snaptask.app.data.local.TaskDao
import com.snaptask.app.data.local.TaskEntity
import com.snaptask.app.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class TaskRepositoryImplTest {

    private lateinit var fakeDao: FakeTaskDao
    private lateinit var scheduledReminders: MutableList<Task>
    private lateinit var cancelledReminders: MutableList<Long>
    private lateinit var deletedImages: MutableList<String>
    private lateinit var onTasksChangedCount: AtomicInteger
    private lateinit var deletedTaskIds: MutableList<Long>
    private lateinit var repository: TaskRepositoryImpl

    private val futureMillis: Long
        get() = System.currentTimeMillis() + 3_600_000L

    private val pastMillis: Long
        get() = System.currentTimeMillis() - 3_600_000L

    @Before
    fun setUp() {
        fakeDao = FakeTaskDao()
        scheduledReminders = Collections.synchronizedList(mutableListOf())
        cancelledReminders = Collections.synchronizedList(mutableListOf())
        deletedImages = Collections.synchronizedList(mutableListOf())
        onTasksChangedCount = AtomicInteger(0)
        deletedTaskIds = Collections.synchronizedList(mutableListOf())

        repository = TaskRepositoryImpl(
            taskDao = fakeDao,
            scheduleReminder = { task -> scheduledReminders.add(task) },
            cancelReminder = { taskId -> cancelledReminders.add(taskId) },
            deleteImages = { paths ->
                paths.filterNotNull().forEach { deletedImages.add(it) }
            },
            onTasksChanged = { onTasksChangedCount.incrementAndGet() },
            onTaskDeleted = { taskId -> deletedTaskIds.add(taskId) }
        )
    }

    @Test
    fun `createTask without reminder persists task updates flow and fires onTasksChanged`() = runTest {
        val task = Task(
            title = "Mua sữa tươi",
            beforeImageUri = "/snaps/before_1.webp",
            createdAt = 1_000L
        )

        val id = repository.createTask(task)

        assertTrue(id > 0L)
        assertEquals(1, onTasksChangedCount.get())
        assertTrue(scheduledReminders.isEmpty())

        val tasks = repository.getAllTasks().first()
        assertEquals(1, tasks.size)
        assertEquals(id, tasks.single().id)
        assertEquals("Mua sữa tươi", tasks.single().title)
    }

    @Test
    fun `createTask with future reminder schedules reminder using assigned id`() = runTest {
        val reminderAt = futureMillis
        val task = Task(
            title = "Gọi điện cho khách hàng",
            reminderTime = reminderAt,
            reminderType = "ALARM",
            createdAt = 2_000L
        )

        val id = repository.createTask(task)

        assertEquals(1, onTasksChangedCount.get())
        assertEquals(1, scheduledReminders.size)
        assertEquals(id, scheduledReminders.single().id)
        assertEquals(reminderAt, scheduledReminders.single().reminderTime)
        assertEquals("ALARM", scheduledReminders.single().reminderType)
    }

    @Test
    fun `createTask with NONE or past reminder does not schedule reminder`() = runTest {
        repository.createTask(
            Task(
                title = "Không nhắc",
                reminderTime = futureMillis,
                reminderType = "NONE",
                createdAt = 1_000L
            )
        )
        repository.createTask(
            Task(
                title = "Quá giờ nhắc",
                reminderTime = pastMillis,
                reminderType = "NOTIFICATION",
                createdAt = 2_000L
            )
        )

        assertTrue(scheduledReminders.isEmpty())
        assertEquals(2, onTasksChangedCount.get())
    }

    @Test
    fun `getAllTasks emits pinned tasks first then newest createdAt first`() = runTest {
        val older = repository.createTask(Task(title = "Cũ", isPinnedToWidget = false, createdAt = 1_000L))
        val newer = repository.createTask(Task(title = "Mới", isPinnedToWidget = false, createdAt = 3_000L))
        val pinned = repository.createTask(Task(title = "Ghim", isPinnedToWidget = true, createdAt = 2_000L))

        val orderedIds = repository.getAllTasks().first().map { it.id }
        assertEquals(listOf(pinned, newer, older), orderedIds)
    }

    @Test
    fun `updateTask updates fields and keeps unchanged images intact`() = runTest {
        val id = repository.createTask(
            Task(
                title = "Bản nháp",
                beforeImageUri = "/snaps/before_keep.webp",
                afterImageUri = "/snaps/after_keep.webp",
                dueDate = 10_000L,
                createdAt = 1_000L
            )
        )
        onTasksChangedCount.set(0)
        cancelledReminders.clear()

        val updated = fakeDao.getTaskById(id)!!.toDomain().copy(
            title = "Tiêu đề mới",
            dueDate = 20_000L
        )
        repository.updateTask(updated)

        assertEquals(1, onTasksChangedCount.get())
        assertTrue(deletedImages.isEmpty())
        val stored = repository.getAllTasks().first().single()
        assertEquals("Tiêu đề mới", stored.title)
        assertEquals(20_000L, stored.dueDate)
    }

    @Test
    fun `updateTask deletes only replaced images and manages reminder scheduling`() = runTest {
        val id = repository.createTask(
            Task(
                title = "Dọn phòng",
                beforeImageUri = "/snaps/before_old.webp",
                afterImageUri = "/snaps/after_keep.webp",
                reminderTime = null,
                reminderType = "NONE",
                createdAt = 1_000L
            )
        )
        scheduledReminders.clear()
        cancelledReminders.clear()
        onTasksChangedCount.set(0)

        // Replace beforeImageUri only and enable future reminder
        val firstUpdate = fakeDao.getTaskById(id)!!.toDomain().copy(
            beforeImageUri = "/snaps/before_new.webp",
            reminderTime = futureMillis,
            reminderType = "NOTIFICATION"
        )
        repository.updateTask(firstUpdate)

        assertEquals(listOf("/snaps/before_old.webp"), deletedImages)
        assertEquals(1, scheduledReminders.size)
        assertTrue(cancelledReminders.isEmpty())
        assertEquals(1, onTasksChangedCount.get())

        // Replace afterImageUri only and disable reminder
        deletedImages.clear()
        scheduledReminders.clear()
        val secondUpdate = fakeDao.getTaskById(id)!!.toDomain().copy(
            afterImageUri = "/snaps/after_new.webp",
            reminderTime = null,
            reminderType = "NONE"
        )
        repository.updateTask(secondUpdate)

        assertEquals(listOf("/snaps/after_keep.webp"), deletedImages)
        assertTrue(scheduledReminders.isEmpty())
        assertEquals(listOf(id), cancelledReminders)
        assertEquals(2, onTasksChangedCount.get())
    }

    @Test
    fun `toggleTaskStatus completes task cancelling reminder and reopens rescheduling future reminder`() = runTest {
        val reminderAt = futureMillis
        val id = repository.createTask(
            Task(
                title = "Tập thể dục",
                reminderTime = reminderAt,
                reminderType = "ALARM",
                createdAt = 1_000L
            )
        )
        scheduledReminders.clear()
        cancelledReminders.clear()
        onTasksChangedCount.set(0)

        // Complete
        repository.toggleTaskStatus(id)
        val completed = fakeDao.getTaskById(id)!!
        assertTrue(completed.isCompleted)
        assertNotNull(completed.completedAt)
        assertEquals(listOf(id), cancelledReminders)
        assertTrue(scheduledReminders.isEmpty())
        assertEquals(1, onTasksChangedCount.get())

        // Reopen with future reminder -> reschedules
        repository.toggleTaskStatus(id)
        val reopened = fakeDao.getTaskById(id)!!
        assertFalse(reopened.isCompleted)
        assertNull(reopened.completedAt)
        assertEquals(1, scheduledReminders.size)
        assertEquals(id, scheduledReminders.single().id)
        assertFalse(scheduledReminders.single().isCompleted)
        assertEquals(2, onTasksChangedCount.get())
    }

    @Test
    fun `toggleTaskStatus on nonexistent id is a no-op and does not fire onTasksChanged`() = runTest {
        repository.toggleTaskStatus(9999L)

        assertEquals(0, onTasksChangedCount.get())
        assertTrue(scheduledReminders.isEmpty())
        assertTrue(cancelledReminders.isEmpty())
    }

    @Test
    fun `toggleTaskStatus reopen with past reminder does not reschedule`() = runTest {
        val id = repository.createTask(
            Task(
                title = "Việc quá hạn nhắc",
                isCompleted = true,
                completedAt = 5_000L,
                reminderTime = pastMillis,
                reminderType = "NOTIFICATION",
                createdAt = 1_000L
            )
        )
        scheduledReminders.clear()
        onTasksChangedCount.set(0)

        repository.toggleTaskStatus(id)

        val reopened = fakeDao.getTaskById(id)!!
        assertFalse(reopened.isCompleted)
        assertNull(reopened.completedAt)
        assertTrue(scheduledReminders.isEmpty())
        assertEquals(1, onTasksChangedCount.get())
    }

    @Test
    fun `completeTaskWithProof marks completed deletes only replaced prior afterImageUri cancels reminder and notifies`() = runTest {
        val id = repository.createTask(
            Task(
                title = "Rửa xe",
                beforeImageUri = "/snaps/before_car.webp",
                afterImageUri = "/snaps/after_car_v1.webp",
                reminderTime = futureMillis,
                reminderType = "NOTIFICATION",
                createdAt = 1_000L
            )
        )
        cancelledReminders.clear()
        onTasksChangedCount.set(0)

        repository.completeTaskWithProof(
            taskId = id,
            afterImageUri = "/snaps/after_car_v2.webp",
            completionCaption = "Sạch bóng rồi!",
            completedAt = 55_000L
        )

        val completed = fakeDao.getTaskById(id)!!
        assertTrue(completed.isCompleted)
        assertEquals("/snaps/before_car.webp", completed.beforeImageUri)
        assertEquals("/snaps/after_car_v2.webp", completed.afterImageUri)
        assertEquals("Sạch bóng rồi!", completed.completionCaption)
        assertEquals(55_000L, completed.completedAt)
        assertEquals(listOf("/snaps/after_car_v1.webp"), deletedImages)
        assertFalse(deletedImages.contains("/snaps/before_car.webp"))
        assertEquals(listOf(id), cancelledReminders)
        assertEquals(1, onTasksChangedCount.get())
    }

    @Test
    fun `completeTaskWithProof without prior afterImageUri never deletes beforeImageUri`() = runTest {
        val id = repository.createTask(
            Task(
                title = "Chụp tài liệu",
                beforeImageUri = "/snaps/before_doc.webp",
                afterImageUri = null,
                createdAt = 1_000L
            )
        )
        onTasksChangedCount.set(0)

        repository.completeTaskWithProof(
            taskId = id,
            afterImageUri = "/snaps/after_doc.webp",
            completionCaption = null,
            completedAt = 60_000L
        )

        val completed = fakeDao.getTaskById(id)!!
        assertTrue(completed.isCompleted)
        assertEquals("/snaps/before_doc.webp", completed.beforeImageUri)
        assertEquals("/snaps/after_doc.webp", completed.afterImageUri)
        assertNull(completed.completionCaption)
        assertTrue(deletedImages.isEmpty())
        assertEquals(listOf(id), cancelledReminders)
        assertEquals(1, onTasksChangedCount.get())
    }

    @Test
    fun `renameFolderTasks ignores blank or identical names and updates matching tasks when valid`() = runTest {
        val workId = repository.createTask(Task(title = "A", folderName = "Công việc", createdAt = 1L))
        val personalId = repository.createTask(Task(title = "B", folderName = "Cá nhân", createdAt = 2L))
        onTasksChangedCount.set(0)

        repository.renameFolderTasks("", "Mới")
        repository.renameFolderTasks("   ", "Mới")
        repository.renameFolderTasks("Công việc", "")
        repository.renameFolderTasks("Công việc", "   ")
        repository.renameFolderTasks("Công việc", "Công việc")

        assertEquals(0, onTasksChangedCount.get())
        assertEquals("Công việc", fakeDao.getTaskById(workId)!!.folderName)
        assertEquals("Cá nhân", fakeDao.getTaskById(personalId)!!.folderName)

        repository.renameFolderTasks("Công việc", "Dự án 2026")

        assertEquals(1, onTasksChangedCount.get())
        assertEquals("Dự án 2026", fakeDao.getTaskById(workId)!!.folderName)
        assertEquals("Cá nhân", fakeDao.getTaskById(personalId)!!.folderName)
    }

    @Test
    fun `deleteTask removes row deletes both images cancels reminder and fires callbacks`() = runTest {
        val id = repository.createTask(
            Task(
                title = "Dọn kho",
                beforeImageUri = "/snaps/before_kho.webp",
                afterImageUri = "/snaps/after_kho.webp",
                createdAt = 1_000L
            )
        )
        cancelledReminders.clear()
        onTasksChangedCount.set(0)
        assertTrue(deletedTaskIds.isEmpty())

        val toDelete = fakeDao.getTaskById(id)!!.toDomain()
        repository.deleteTask(toDelete)

        assertNull(fakeDao.getTaskById(id))
        assertTrue(repository.getAllTasks().first().isEmpty())
        assertEquals(setOf("/snaps/before_kho.webp", "/snaps/after_kho.webp"), deletedImages.toSet())
        assertEquals(2, deletedImages.size)
        assertEquals(listOf(id), cancelledReminders)
        assertEquals(listOf(id), deletedTaskIds)
        assertEquals(1, onTasksChangedCount.get())
    }

    private class FakeTaskDao : TaskDao {
        private val lock = Any()
        private val tasks = linkedMapOf<Long, TaskEntity>()
        private var nextId = 1L
        private val flow = MutableStateFlow<List<TaskEntity>>(emptyList())

        private fun publishLocked() {
            flow.value = tasks.values.sortedWith(
                compareByDescending<TaskEntity> { it.isPinnedToWidget }
                    .thenByDescending { it.createdAt }
            )
        }

        override fun getAllTasksFlow(): Flow<List<TaskEntity>> = flow

        override suspend fun getTaskById(id: Long): TaskEntity? = synchronized(lock) {
            tasks[id]
        }

        override suspend fun getPendingTasksForWidget(): List<TaskEntity> = synchronized(lock) {
            tasks.values
                .filter { !it.isCompleted }
                .sortedWith(
                    compareByDescending<TaskEntity> { it.isPinnedToWidget }
                        .thenByDescending { it.createdAt }
                )
                .take(5)
        }

        override suspend fun getPendingTasksWithFutureReminder(now: Long): List<TaskEntity> = synchronized(lock) {
            tasks.values
                .filter { entity ->
                    val reminderAt = entity.reminderTime
                    !entity.isCompleted &&
                        reminderAt != null &&
                        reminderAt > now &&
                        entity.reminderType != "NONE"
                }
                .sortedBy { it.reminderTime }
        }

        override suspend fun getCompletedCount(): Int = synchronized(lock) {
            tasks.values.count { it.isCompleted }
        }

        override suspend fun getTotalCount(): Int = synchronized(lock) {
            tasks.size
        }

        override suspend fun insertTask(task: TaskEntity): Long {
            return synchronized(lock) {
                val assignedId = if (task.id == 0L) nextId++ else {
                    if (task.id >= nextId) nextId = task.id + 1
                    task.id
                }
                tasks[assignedId] = task.copy(id = assignedId)
                publishLocked()
                assignedId
            }
        }

        override suspend fun updateTask(task: TaskEntity) {
            synchronized(lock) {
                if (tasks.containsKey(task.id)) {
                    tasks[task.id] = task
                    publishLocked()
                }
            }
        }

        override suspend fun completeTask(
            id: Long,
            isCompleted: Boolean,
            afterUri: String?,
            caption: String?,
            completedAt: Long?
        ) {
            synchronized(lock) {
                val current = tasks[id] ?: return
                tasks[id] = current.copy(
                    isCompleted = isCompleted,
                    afterImageUri = afterUri,
                    completionCaption = caption,
                    completedAt = completedAt
                )
                publishLocked()
            }
        }

        override suspend fun toggleCompleted(id: Long, isCompleted: Boolean, completedAt: Long?) {
            synchronized(lock) {
                val current = tasks[id] ?: return
                tasks[id] = current.copy(isCompleted = isCompleted, completedAt = completedAt)
                publishLocked()
            }
        }

        override suspend fun updateTaskReminder(id: Long, reminderTime: Long?, reminderType: String) {
            synchronized(lock) {
                val current = tasks[id] ?: return
                tasks[id] = current.copy(reminderTime = reminderTime, reminderType = reminderType)
                publishLocked()
            }
        }

        override suspend fun renameFolderTasks(oldName: String, newName: String) {
            synchronized(lock) {
                var changed = false
                for ((id, entity) in tasks.entries.toList()) {
                    if (entity.folderName == oldName) {
                        tasks[id] = entity.copy(folderName = newName)
                        changed = true
                    }
                }
                if (changed) {
                    publishLocked()
                }
            }
        }

        override suspend fun deleteTask(task: TaskEntity) {
            synchronized(lock) {
                if (tasks.remove(task.id) != null) {
                    publishLocked()
                }
            }
        }
    }
}
