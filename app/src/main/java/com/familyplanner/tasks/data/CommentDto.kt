package com.familyplanner.tasks.data

data class CommentDto(
    val id: String,
    val userId: String,
    val userName: String,
    val createdAt: Long,
    val text: String,
    val fileNames: List<String>
)