package com.veducation.app

data class Study(
    val id: String,
    val title: String,
    val description: String?,
    val content: String,
    val studyType: String,
    val subjectId: String,
    val authorId: String,
    val authorName: String,
    val authorImageUrl: String?,
    var upvotesCount: Int,
    var downvotesCount: Int,
    val commentsCount: Int,
    val viewsCount: Int,
    val createdAt: String,
    val updatedAt: String,
    var userVote: String?, // "upvote", "downvote", or null
    var isSaved: Boolean
)
