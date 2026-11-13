package com.snaptask.app.data.local

import androidx.room.TypeConverter
import com.snaptask.app.domain.model.SubTask
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromSubTaskList(subtasks: List<SubTask>?): String = encodeSubTaskList(subtasks)

    @TypeConverter
    fun toSubTaskList(jsonString: String?): List<SubTask> = decodeSubTaskList(jsonString)

    companion object {
        fun encodeSubTaskList(subtasks: List<SubTask>?): String {
            if (subtasks.isNullOrEmpty()) return "[]"
            val array = JSONArray()
            for (item in subtasks) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("isDone", item.isDone)
                array.put(obj)
            }
            return array.toString()
        }

        fun decodeSubTaskList(jsonString: String?): List<SubTask> {
            if (jsonString.isNullOrBlank()) return emptyList()
            val list = mutableListOf<SubTask>()
            try {
                val array = JSONArray(jsonString.trim())
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val rawId = if (obj.isNull("id")) "" else obj.optString("id", "")
                    val id = rawId.ifBlank { i.toString() }
                    val title = if (obj.isNull("title")) "" else obj.optString("title", "")
                    val isDone = obj.optBoolean("isDone", false)
                    list.add(
                        SubTask(
                            id = id,
                            title = title,
                            isDone = isDone
                        )
                    )
                }
            } catch (_: Exception) {
                // Fallback gracefully on parsing error
            }
            return list
        }
    }
}
