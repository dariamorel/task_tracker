package ru.hse.tracker

data class Tag(val id: Long? = null, val name: String)

data class Task(
    val id: Long? = null,
    val title: String,
    val done: Boolean = false,
    val priority: String = "MEDIUM",
    val tagId: Long? = null,
)

data class ErrorResponse(val error: String)

val PRIORITIES = setOf("LOW", "MEDIUM", "HIGH")
