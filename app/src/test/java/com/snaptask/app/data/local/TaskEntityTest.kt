package com.snaptask.app.data.local

import com.snaptask.app.domain.model.SubTask
import com.snaptask.app.domain.model.Task
import com.snaptask.app.domain.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskEntityTest {

    @Test
    fun `toDomain and fromDomain perform lossless full-field round trip`() {
        val subTasksJson = """[{"id":"1","title":"Dọn bàn học","isDone":true}]"""
        val entity = TaskEntity(
            id = 42L,
            title = "Ôn thi môn Hệ điều hành",
            description = "Xem lại chương deadlock và bộ nhớ ảo",
            beforeImageUri = "/data/user/0/com.snaptask.app/files/snaps/before_42.webp",
            afterImageUri = "/data/user/0/com.snaptask.app/files/snaps/after_42.webp",
            completionCaption = "Đã làm xong toàn bộ đề cương!",
            isCompleted = true,
            isPinnedToWidget = true,
            isFavorite = true,
            folderName = "Học tập",
            priority = "HIGH",
            tag = "STUDY",
            subTasksJson = subTasksJson,
            createdAt = 1_700_000_000_000L,
            dueDate = 1_700_086_400_000L,
            completedAt = 1_700_050_000_000L,
            reminderTime = 1_700_082_800_000L,
            reminderType = "ALARM"
        )

        val domain = entity.toDomain()

        assertEquals(42L, domain.id)
        assertEquals(entity.title, domain.title)
        assertEquals(entity.description, domain.description)
        assertEquals(entity.beforeImageUri, domain.beforeImageUri)
        assertEquals(entity.afterImageUri, domain.afterImageUri)
        assertEquals(entity.completionCaption, domain.completionCaption)
        assertTrue(domain.isCompleted)
        assertTrue(domain.isPinnedToWidget)
        assertTrue(domain.isFavorite)
        assertEquals("Học tập", domain.folderName)
        assertEquals("HIGH", domain.priority)
        assertEquals("STUDY", domain.tag)
        assertEquals(subTasksJson, domain.subTasksJson)
        assertEquals(1_700_000_000_000L, domain.createdAt)
        assertEquals(1_700_086_400_000L, domain.dueDate)
        assertEquals(1_700_050_000_000L, domain.completedAt)
        assertEquals(1_700_082_800_000L, domain.reminderTime)
        assertEquals("ALARM", domain.reminderType)

        val roundTripped = TaskEntity.fromDomain(domain)
        assertEquals(entity, roundTripped)
    }

    @Test
    fun `toDomain and fromDomain preserve nullable fields when unset`() {
        val entity = TaskEntity(
            id = 7L,
            title = "Ghi chú nhanh",
            createdAt = 1_700_000_000_000L
        )

        val domain = entity.toDomain()
        assertNull(domain.afterImageUri)
        assertNull(domain.completionCaption)
        assertNull(domain.dueDate)
        assertNull(domain.completedAt)
        assertNull(domain.reminderTime)
        assertEquals("NONE", domain.reminderType)
        assertEquals(entity, TaskEntity.fromDomain(domain))
    }

    @Test
    fun `priorityEnum maps valid values case-insensitively with trim and defaults invalid to MEDIUM`() {
        assertEquals(TaskPriority.LOW, Task(title = "t", priority = "LOW").priorityEnum)
        assertEquals(TaskPriority.MEDIUM, Task(title = "t", priority = "MEDIUM").priorityEnum)
        assertEquals(TaskPriority.HIGH, Task(title = "t", priority = "HIGH").priorityEnum)

        assertEquals(TaskPriority.LOW, Task(title = "t", priority = "low").priorityEnum)
        assertEquals(TaskPriority.HIGH, Task(title = "t", priority = "  high\t").priorityEnum)
        assertEquals(TaskPriority.MEDIUM, Task(title = "t", priority = " Medium ").priorityEnum)

        assertEquals(TaskPriority.MEDIUM, Task(title = "t", priority = "").priorityEnum)
        assertEquals(TaskPriority.MEDIUM, Task(title = "t", priority = "   ").priorityEnum)
        assertEquals(TaskPriority.MEDIUM, Task(title = "t", priority = "CRITICAL").priorityEnum)
        assertEquals(TaskPriority.MEDIUM, Task(title = "t", priority = "invalid").priorityEnum)
    }

    @Test
    fun `hasImage and hasProof reflect image paths and completion state`() {
        assertFalse(Task(title = "no img", beforeImageUri = "").hasImage)
        assertFalse(Task(title = "blank img", beforeImageUri = "   ").hasImage)
        assertTrue(Task(title = "with img", beforeImageUri = "/snaps/before.webp").hasImage)

        assertFalse(Task(title = "incomplete", isCompleted = false, afterImageUri = "/snaps/after.webp").hasProof)
        assertFalse(Task(title = "null proof", isCompleted = true, afterImageUri = null).hasProof)
        assertFalse(Task(title = "blank proof", isCompleted = true, afterImageUri = "   ").hasProof)
        assertTrue(Task(title = "valid proof", isCompleted = true, afterImageUri = "/snaps/after.webp").hasProof)
    }

    @Test
    fun `hasReminder requires future time incomplete status and non-NONE reminderType`() {
        val future = System.currentTimeMillis() + 3_600_000L
        val past = System.currentTimeMillis() - 3_600_000L

        assertTrue(
            Task(
                title = "future notification",
                isCompleted = false,
                reminderTime = future,
                reminderType = "NOTIFICATION"
            ).hasReminder
        )
        assertTrue(
            Task(
                title = "future alarm",
                isCompleted = false,
                reminderTime = future,
                reminderType = "ALARM"
            ).hasReminder
        )
        assertFalse(
            Task(
                title = "none upper",
                isCompleted = false,
                reminderTime = future,
                reminderType = "NONE"
            ).hasReminder
        )
        assertFalse(
            Task(
                title = "none lower",
                isCompleted = false,
                reminderTime = future,
                reminderType = "none"
            ).hasReminder
        )
        assertFalse(
            Task(
                title = "completed",
                isCompleted = true,
                reminderTime = future,
                reminderType = "NOTIFICATION"
            ).hasReminder
        )
        assertFalse(
            Task(
                title = "past",
                isCompleted = false,
                reminderTime = past,
                reminderType = "NOTIFICATION"
            ).hasReminder
        )
        assertFalse(
            Task(
                title = "null time",
                isCompleted = false,
                reminderTime = null,
                reminderType = "NOTIFICATION"
            ).hasReminder
        )
    }

    @Test
    fun `parsedSubtasks decodes subTasksJson and falls back on malformed json`() {
        val task = Task(
            title = "Checklist",
            subTasksJson = """[{"id":"a","title":"Bước 1","isDone":false},{"id":"b","title":"Bước 2","isDone":true}]"""
        )
        assertEquals(
            listOf(
                SubTask(id = "a", title = "Bước 1", isDone = false),
                SubTask(id = "b", title = "Bước 2", isDone = true)
            ),
            task.parsedSubtasks
        )

        assertEquals(emptyList<SubTask>(), Task(title = "Empty", subTasksJson = "[]").parsedSubtasks)
        assertEquals(emptyList<SubTask>(), Task(title = "Broken", subTasksJson = "{broken").parsedSubtasks)
    }
}
