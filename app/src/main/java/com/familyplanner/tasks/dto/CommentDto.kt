package com.familyplanner.tasks.dto

import com.google.type.DateTime

data class CommentDto(
    val id: String,
    val userId: String,
    val userName: String,
    val createdAt: DateTime,
    val text: String,
    val fileNames: List<String>
)