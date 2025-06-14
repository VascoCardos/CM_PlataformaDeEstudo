package com.veducation.app

data class Subject(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    var followersCount: Int,
    var isFollowed: Boolean,
    val isFeatured: Boolean,
    val difficultyLevel: Int,
    val estimatedHours: Int,
    val categoryName: String,
    val categoryColor: String
)
