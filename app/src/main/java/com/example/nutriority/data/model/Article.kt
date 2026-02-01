package com.example.nutriority.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val date: String = "",
    val category: String,
    val content: String,
    val description: String = "",
    val imageName: String, // stored as URL from Firestore
    val articleUrl: String = "",
    val source: String = ""
)
