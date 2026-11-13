package com.snaptask.app.data.local

import com.snaptask.app.domain.model.SubTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `toSubTaskList returns emptyList for null empty blank and empty array`() {
        assertEquals(emptyList<SubTask>(), converters.toSubTaskList(null))
        assertEquals(emptyList<SubTask>(), converters.toSubTaskList(""))
        assertEquals(emptyList<SubTask>(), converters.toSubTaskList("   \n\t  "))
        assertEquals(emptyList<SubTask>(), converters.toSubTaskList("[]"))
        assertEquals(emptyList<SubTask>(), converters.toSubTaskList("  []  "))
    }

    @Test
    fun `fromSubTaskList returns empty json array for null or empty list`() {
        assertEquals("[]", converters.fromSubTaskList(null))
        assertEquals("[]", converters.fromSubTaskList(emptyList()))
    }

    @Test
    fun `toSubTaskList handles malformed json and non-object array elements gracefully`() {
        assertEquals(emptyList<SubTask>(), converters.toSubTaskList("{bad"))
        assertEquals(emptyList<SubTask>(), converters.toSubTaskList("{\"id\":\"1\",\"title\":\"x\"}"))
        assertEquals(emptyList<SubTask>(), converters.toSubTaskList("[123]"))
        assertEquals(emptyList<SubTask>(), converters.toSubTaskList("not-json"))

        val mixedJson = """[123, null, "text", {"id":"valid-1","title":"Hợp lệ","isDone":true}]"""
        assertEquals(
            listOf(SubTask(id = "valid-1", title = "Hợp lệ", isDone = true)),
            converters.toSubTaskList(mixedJson)
        )
    }

    @Test
    fun `round trip preserves ids titles completion state order and Vietnamese Unicode`() {
        val original = listOf(
            SubTask(id = "sub-1", title = "Mua cà phê sữa đá", isDone = false),
            SubTask(id = "sub-2", title = "Hoàn thiện báo cáo quý 3 — Đã kiểm tra", isDone = true),
            SubTask(id = "sub-3", title = "Gửi tài liệu cho anh Tuấn (ưu tiên)", isDone = false)
        )

        val serialized = converters.fromSubTaskList(original)
        val restored = converters.toSubTaskList(serialized)

        assertEquals(original, restored)
        assertEquals(listOf("sub-1", "sub-2", "sub-3"), restored.map { it.id })
        assertFalse(restored[0].isDone)
        assertTrue(restored[1].isDone)
        assertFalse(restored[2].isDone)
    }

    @Test
    fun `toSubTaskList applies fallback defaults for null and partial item fields`() {
        val partialJson = """
            [
              {"id": null, "title": null, "isDone": null},
              {},
              {"id": "   ", "title": "Chỉ có tiêu đề"},
              {"id": "custom-id", "isDone": true}
            ]
        """.trimIndent()

        val result = converters.toSubTaskList(partialJson)

        assertEquals(4, result.size)
        assertEquals(SubTask(id = "0", title = "", isDone = false), result[0])
        assertEquals(SubTask(id = "1", title = "", isDone = false), result[1])
        assertEquals(SubTask(id = "2", title = "Chỉ có tiêu đề", isDone = false), result[2])
        assertEquals(SubTask(id = "custom-id", title = "", isDone = true), result[3])
    }

    @Test
    fun `companion decodeSubTaskList handles null blank and malformed JSON safely`() {
        assertEquals(emptyList<SubTask>(), Converters.decodeSubTaskList(null))
        assertEquals(emptyList<SubTask>(), Converters.decodeSubTaskList(""))
        assertEquals(emptyList<SubTask>(), Converters.decodeSubTaskList("   \n\t  "))
        assertEquals(emptyList<SubTask>(), Converters.decodeSubTaskList("{broken"))
        assertEquals(emptyList<SubTask>(), Converters.decodeSubTaskList("{\"id\":\"1\"}"))
        assertEquals(emptyList<SubTask>(), Converters.decodeSubTaskList("[123, \"text\"]"))
    }

    @Test
    fun `companion encodeSubTaskList and decodeSubTaskList handle empty and valid round-trip`() {
        assertEquals("[]", Converters.encodeSubTaskList(null))
        assertEquals("[]", Converters.encodeSubTaskList(emptyList()))

        val subtasks = listOf(
            SubTask(id = "a1", title = "Mua cà phê sữa đá", isDone = false),
            SubTask(id = "a2", title = "Gửi báo cáo tiến độ tuần 2", isDone = true)
        )

        val json = Converters.encodeSubTaskList(subtasks)
        assertEquals(subtasks, Converters.decodeSubTaskList(json))
        assertEquals(subtasks, converters.toSubTaskList(json))

        val partial = Converters.decodeSubTaskList("[{\"title\":\"Thiếu id\"},{\"id\":null,\"isDone\":true}]")
        assertEquals(
            listOf(
                SubTask(id = "0", title = "Thiếu id", isDone = false),
                SubTask(id = "1", title = "", isDone = true)
            ),
            partial
        )
    }
}
