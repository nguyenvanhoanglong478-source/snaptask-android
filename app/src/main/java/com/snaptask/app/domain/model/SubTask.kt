package com.snaptask.app.domain.model

import java.util.UUID

data class SubTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false
)
