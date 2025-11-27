package com.example.nutriority.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val author: String,
    val readingTime: String,
    val category: String,
    val imageName: String // stored in DB
) {
    @Ignore // tell Room to ignore this property
    var imageResId: Int = 0 // runtime-only, not stored in DB
}
