package com.snaptask.app.domain.model

enum class TaskPriority(val label: String, val level: Int) {
    LOW("Thấp", 1),
    MEDIUM("Vừa", 2),
    HIGH("Gấp", 3);

    companion object {
        fun fromLevel(level: Int): TaskPriority =
            entries.find { it.level == level } ?: MEDIUM
    }
}
