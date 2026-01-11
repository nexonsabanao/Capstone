package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val author: String,
    val readingTime: String,
    val category: String,
    val content: String,
    val imageName: String // stored in DB
) {
    @Ignore // tell Room to ignore this property
    var imageResId: Int = 0 // runtime-only, not stored in DB
}