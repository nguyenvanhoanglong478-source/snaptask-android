package com.snaptask.app.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class CustomFolder(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val isDefault: Boolean
)

enum class RepeatCadence(
    val key: String,
    val chipLabel: String,
    val badgeLabel: String
) {
    NONE("NONE", "Lặp lại", ""),
    DAILY("DAILY", "Hằng ngày", "Hằng ngày"),
    EVERY_3_DAYS("EVERY_3_DAYS", "3 ngày", "Mỗi 3 ngày"),
    WEEKLY("WEEKLY", "Hằng tuần", "Hằng tuần");

    fun next(): RepeatCadence = when (this) {
        NONE -> DAILY
        DAILY -> EVERY_3_DAYS
        EVERY_3_DAYS -> WEEKLY
        WEEKLY -> NONE
    }

    fun toTaskTag(): String = if (this == NONE) "PERSONAL" else "REPEAT_${key}"

    companion object {
        fun fromKeyOrTag(value: String?): RepeatCadence {
            if (value.isNullOrBlank()) return NONE
            val cleaned = value.removePrefix("REPEAT_")
            return entries.firstOrNull { it.key.equals(cleaned, ignoreCase = true) } ?: NONE
        }
    }
}

data class FolderColorPreset(val hex: Long, val label: String)

data class FolderConfig(
    val folders: List<CustomFolder>,
    val smartUrgentEnabled: Boolean,
    val smartProofEnabled: Boolean,
    val repeatByTaskId: Map<Long, RepeatCadence>
) {
    val defaultFolder: CustomFolder?
        get() = folders.firstOrNull { it.isDefault } ?: folders.firstOrNull()
}

// Persists custom folders, smart folder switches, and repeat cadence without altering Room schema v3.
class FolderPreferencesRepository(context: Context) {

    companion object {
        const val UNASSIGNED_FOLDER_NAME = "Không thư mục"

        val ICON_KEYS = listOf("home", "brief", "heart", "wrench", "cart", "leaf", "book", "bike")

        val COLOR_PRESETS = listOf(
            FolderColorPreset(0xFFD4FF3A, "Chanh"),
            FolderColorPreset(0xFFFF7A45, "Cam đất"),
            FolderColorPreset(0xFF38D399, "Xanh lá"),
            FolderColorPreset(0xFF5CA8FF, "Xanh dương"),
            FolderColorPreset(0xFFB68CFF, "Tím nhạt"),
            FolderColorPreset(0xFFB0AFA7, "Trung tính")
        )

        private const val PREFS_NAME = "snaptask_snappop_prefs"
        private const val KEY_FOLDERS_JSON = "custom_folders_json"
        private const val KEY_SMART_URGENT = "smart_urgent_enabled"
        private const val KEY_SMART_PROOF = "smart_proof_enabled"
        private const val KEY_REPEAT_JSON = "task_repeat_json"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadFromDisk())
    val configFlow: StateFlow<FolderConfig> = _configFlow.asStateFlow()

    private fun defaultSeedFolders(): List<CustomFolder> = listOf(
        CustomFolder(id = "nha", name = "Nhà", iconKey = "home", colorHex = 0xFFD4FF3A, isDefault = true),
        CustomFolder(id = "congviec", name = "Công việc", iconKey = "brief", colorHex = 0xFF5CA8FF, isDefault = false),
        CustomFolder(id = "canhan", name = "Cá nhân", iconKey = "leaf", colorHex = 0xFF38D399, isDefault = false),
        CustomFolder(id = "giadinh", name = "Gia đình", iconKey = "heart", colorHex = 0xFFFF7A45, isDefault = false)
    )

    private fun loadFromDisk(): FolderConfig {
        val rawFolders = prefs.getString(KEY_FOLDERS_JSON, null)
        val folders = if (rawFolders == null) {
            val seed = defaultSeedFolders()
            persistFolders(seed)
            seed
        } else {
            runCatching {
                val arr = JSONArray(rawFolders)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        add(
                            CustomFolder(
                                id = obj.optString("id", "f_$i"),
                                name = obj.optString("name", "Thư mục"),
                                iconKey = obj.optString("iconKey", "home"),
                                colorHex = obj.optLong("colorHex", 0xFFD4FF3A),
                                isDefault = obj.optBoolean("isDefault", false)
                            )
                        )
                    }
                }
            }.getOrElse { defaultSeedFolders() }
        }

        val rawRepeat = prefs.getString(KEY_REPEAT_JSON, "{}") ?: "{}"
        val repeatMap = runCatching {
            val obj = JSONObject(rawRepeat)
            val map = mutableMapOf<Long, RepeatCadence>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val taskId = k.toLongOrNull() ?: continue
                val cadence = RepeatCadence.fromKeyOrTag(obj.optString(k))
                if (cadence != RepeatCadence.NONE) {
                    map[taskId] = cadence
                }
            }
            map.toMap()
        }.getOrElse { emptyMap() }

        return FolderConfig(
            folders = ensureSingleDefault(folders),
            smartUrgentEnabled = prefs.getBoolean(KEY_SMART_URGENT, true),
            smartProofEnabled = prefs.getBoolean(KEY_SMART_PROOF, true),
            repeatByTaskId = repeatMap
        )
    }

    private fun ensureSingleDefault(list: List<CustomFolder>): List<CustomFolder> {
        if (list.isEmpty()) return list
        val firstDefIdx = list.indexOfFirst { it.isDefault }.let { if (it >= 0) it else 0 }
        return list.mapIndexed { idx, f -> f.copy(isDefault = idx == firstDefIdx) }
    }

    private fun persistFolders(list: List<CustomFolder>) {
        val arr = JSONArray()
        list.forEach { f ->
            arr.put(
                JSONObject().apply {
                    put("id", f.id)
                    put("name", f.name)
                    put("iconKey", f.iconKey)
                    put("colorHex", f.colorHex)
                    put("isDefault", f.isDefault)
                }
            )
        }
        prefs.edit().putString(KEY_FOLDERS_JSON, arr.toString()).apply()
    }

    private fun persistRepeats(map: Map<Long, RepeatCadence>) {
        val obj = JSONObject()
        map.forEach { (id, cadence) ->
            if (cadence != RepeatCadence.NONE) {
                obj.put(id.toString(), cadence.key)
            }
        }
        prefs.edit().putString(KEY_REPEAT_JSON, obj.toString()).apply()
    }

    fun setSmartUrgentEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_URGENT, enabled).apply()
        _configFlow.value = _configFlow.value.copy(smartUrgentEnabled = enabled)
    }

    fun setSmartProofEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_PROOF, enabled).apply()
        _configFlow.value = _configFlow.value.copy(smartProofEnabled = enabled)
    }

    fun moveFolder(index: Int, delta: Int) {
        val current = _configFlow.value.folders.toMutableList()
        val target = index + delta
        if (index !in current.indices || target !in current.indices) return
        val tmp = current[index]
        current[index] = current[target]
        current[target] = tmp
        persistFolders(current)
        _configFlow.value = _configFlow.value.copy(folders = current)
    }

    fun saveFolder(
        editingId: String?,
        rawName: String,
        iconKey: String,
        colorHex: Long,
        makeDefault: Boolean
    ): Pair<String?, String> {
        val cleanName = rawName.trim().ifBlank { "Thư mục mới" }
        val current = _configFlow.value.folders.toMutableList()
        var oldNameIfRenamed: String? = null

        val cleared = if (makeDefault) {
            current.map { it.copy(isDefault = false) }.toMutableList()
        } else current

        if (editingId != null) {
            val idx = cleared.indexOfFirst { it.id == editingId }
            if (idx >= 0) {
                val prev = cleared[idx]
                if (!prev.name.equals(cleanName, ignoreCase = false)) {
                    oldNameIfRenamed = prev.name
                }
                cleared[idx] = prev.copy(
                    name = cleanName,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    isDefault = makeDefault || prev.isDefault
                )
            }
        } else {
            val existingSameName = cleared.indexOfFirst { it.name.equals(cleanName, ignoreCase = true) }
            if (existingSameName >= 0) {
                val prev = cleared[existingSameName]
                cleared[existingSameName] = prev.copy(
                    iconKey = iconKey,
                    colorHex = colorHex,
                    isDefault = makeDefault || prev.isDefault
                )
            } else {
                cleared.add(
                    CustomFolder(
                        id = "f_${System.currentTimeMillis()}",
                        name = cleanName,
                        iconKey = iconKey,
                        colorHex = colorHex,
                        isDefault = makeDefault || cleared.isEmpty()
                    )
                )
            }
        }

        val normalized = ensureSingleDefault(cleared)
        persistFolders(normalized)
        _configFlow.value = _configFlow.value.copy(folders = normalized)
        return Pair(oldNameIfRenamed, cleanName)
    }

    fun deleteFolder(id: String): String? {
        val current = _configFlow.value.folders
        val target = current.firstOrNull { it.id == id } ?: return null
        val remaining = ensureSingleDefault(current.filterNot { it.id == id })
        persistFolders(remaining)
        _configFlow.value = _configFlow.value.copy(folders = remaining)
        return target.name
    }

    fun syncDiscoveredFolders(taskFolderNames: List<String>) {
        val current = _configFlow.value.folders.toMutableList()
        var changed = false
        for (raw in taskFolderNames) {
            val name = raw.trim()
            if (name.isBlank() || name.equals(UNASSIGNED_FOLDER_NAME, ignoreCase = true)) continue
            if (current.none { it.name.equals(name, ignoreCase = true) }) {
                val color = COLOR_PRESETS[current.size % COLOR_PRESETS.size].hex
                val icon = ICON_KEYS[current.size % ICON_KEYS.size]
                current.add(
                    CustomFolder(
                        id = "disc_${name.lowercase().hashCode()}",
                        name = name,
                        iconKey = icon,
                        colorHex = color,
                        isDefault = current.isEmpty()
                    )
                )
                changed = true
            }
        }
        if (changed) {
            val normalized = ensureSingleDefault(current)
            persistFolders(normalized)
            _configFlow.value = _configFlow.value.copy(folders = normalized)
        }
    }

    fun setTaskRepeat(taskId: Long, cadence: RepeatCadence) {
        if (taskId <= 0L) return
        val next = _configFlow.value.repeatByTaskId.toMutableMap()
        if (cadence == RepeatCadence.NONE) {
            next.remove(taskId)
        } else {
            next[taskId] = cadence
        }
        persistRepeats(next)
        _configFlow.value = _configFlow.value.copy(repeatByTaskId = next)
    }
}
