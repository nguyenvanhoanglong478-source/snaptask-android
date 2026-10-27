package com.snaptask.app.domain.model

enum class TaskStatus(val label: String) {
    TODO("Cần làm"),
    IN_PROGRESS("Đang làm"),
    DONE("Đã xong");

    fun next(): TaskStatus = when (this) {
        TODO -> IN_PROGRESS
        IN_PROGRESS -> DONE
        DONE -> TODO
    }
}
