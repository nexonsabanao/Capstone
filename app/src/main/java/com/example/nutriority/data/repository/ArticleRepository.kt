package com.example.nutriority.data.repository

import com.example.nutriority.data.model.Article
import com.example.nutriority.data.local.ArticlesDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Article data.
 * It abstracts the data source (the ArticlesDao) from the rest of the app.
 * This class is the single source of truth for all article-related data.
 */
class ArticleRepository(private val articlesDao: ArticlesDao) {

    /**
     * A Flow that emits a list of all articles from the database, ordered by title.
     * The UI can collect this Flow to reactively update when the data changes.
     */
    val allArticles: Flow<List<Article>> = articlesDao.getAllArticles()

    /**
     * Retrieves a single article by its ID.
     * @param articleId The ID of the article to fetch.
     * @return A Flow that emits the specific Article object, or null if not found.
     */
    fun getArticleById(articleId: Int): Flow<Article?> {
        return articlesDao.getArticleById(articleId)
    }

    /**
     * Inserts a new article into the database.
     * This is a suspend function and must be called from a coroutine scope.
     * @param article The article object to insert.
     */
    suspend fun insert(article: Article) {
        articlesDao.insertArticle(article)
    }

    /**
     * Inserts a list of articles into the database in a single transaction.
     * This is a suspend function and must be called from a coroutine scope.
     * @param articles The list of article objects to insert.
     */
    suspend fun insertAll(articles: List<Article>) {
        // FIX: Changed to call the correct DAO function name
        articlesDao.insertAllArticles(articles)
    }

    /**
     * Updates an existing article in the database.
     * This is a suspend function and must be called from a coroutine scope.
     * @param article The article object to update.
     */
    suspend fun update(article: Article) {
        articlesDao.updateArticle(article)
    }

    /**
     * Deletes a specific article from the database.
     * This is a suspend function and must be called from a coroutine scope.
     * @param article The article object to delete.
     */
    suspend fun delete(article: Article) {
        articlesDao.deleteArticle(article)
    }

    /**
     * Deletes all articles from the database.
     * This is a suspend function and must be called from a coroutine scope.
     */
    suspend fun deleteAll() {
        articlesDao.deleteAllArticles()
    }
}
