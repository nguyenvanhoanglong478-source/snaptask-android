package com.snaptask.app.data.local

import androidx.room.TypeConverter
import com.snaptask.app.domain.model.SubTask
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromSubTaskList(subtasks: List<SubTask>?): String {
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

    @TypeConverter
    fun toSubTaskList(jsonString: String?): List<SubTask> {
        if (jsonString.isNullOrBlank()) return emptyList()
        val list = mutableListOf<SubTask>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SubTask(
                        id = obj.optString("id", i.toString()),
                        title = obj.optString("title", ""),
                        isDone = obj.optBoolean("isDone", false)
                    )
                )
            }
        } catch (_: Exception) {
            // Fallback gracefully on parsing error
        }
        return list
    }
}
