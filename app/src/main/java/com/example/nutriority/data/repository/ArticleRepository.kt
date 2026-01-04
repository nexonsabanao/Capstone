package com.example.nutriority.data.repository

import android.app.Application
import com.example.nutriority.data.model.Article
import com.example.nutriority.data.local.ArticlesDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArticleRepository(private val articlesDao: ArticlesDao, private val application: Application) {

    val allArticles: Flow<List<Article>> = articlesDao.getAllArticles().map { articles ->
        articles.map {
            it.apply {
                val resources = application.resources
                val packageName = application.packageName
                imageResId = resources.getIdentifier(it.imageName, "drawable", packageName)
            }
        }
    }

    fun getArticleById(articleId: Int): Flow<Article?> {
        return articlesDao.getArticleById(articleId)
    }

    suspend fun insert(article: Article) {
        articlesDao.insertArticle(article)
    }

    suspend fun insertAll(articles: List<Article>) {
        articlesDao.insertAllArticles(articles)
    }

    suspend fun update(article: Article) {
        articlesDao.updateArticle(article)
    }

    suspend fun delete(article: Article) {
        articlesDao.deleteArticle(article)
    }

    suspend fun deleteAll() {
        articlesDao.deleteAllArticles()
    }
}
