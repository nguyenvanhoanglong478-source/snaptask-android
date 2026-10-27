package com.snaptask.app.domain.model

enum class TaskTag(val displayName: String, val hexColor: Long) {
    WORK("Công việc", 0xFF2563EB),     // Blue
    PERSONAL("Cá nhân", 0xFF059669),   // Green
    SHOPPING("Mua sắm", 0xFFD97706),   // Amber
    HOME("Nhà cửa", 0xFF7C3AED),       // Purple/Violet
    STUDY("Học tập", 0xFFDC2626),      // Red
    OTHER("Khác", 0xFF4B5563);         // Gray
}
