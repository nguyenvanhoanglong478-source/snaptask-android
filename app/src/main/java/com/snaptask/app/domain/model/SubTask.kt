package com.snaptask.app.domain.model

data class SubTask(
    val id: String,
    val title: String,
    val isDone: Boolean = false
)
