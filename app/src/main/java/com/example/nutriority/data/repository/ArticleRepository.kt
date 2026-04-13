package com.example.nutriority.data.repository

import android.app.Application
import android.util.Log
import com.example.nutriority.data.local.ArticlesDao
import com.example.nutriority.data.model.Article
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ArticleRepository(
    private val articlesDao: ArticlesDao,
    private val application: Application
) {
    private val db = FirebaseFirestore.getInstance()

    val allArticles: Flow<List<Article>> = articlesDao.getAllArticles()

    fun getArticleById(articleId: String): Flow<Article?> {
        return articlesDao.getArticleById(articleId)
    }

    /**
     * Starts a live Firestore listener to keep the local article library updated in real-time.
     */
    fun startRealtimeArticleSync(scope: CoroutineScope) {
        db.collection("articles").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("ArticleRepo", "Article sync failed", e)
                return@addSnapshotListener
            }
            
            snapshot?.let {
                val articles = it.documents.mapNotNull { doc ->
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val author = doc.getString("author") ?: ""
                    val category = doc.getString("category") ?: ""
                    val content = doc.getString("content") ?: ""
                    val imageName = doc.getString("imageName") ?: ""
                    val date = doc.getString("date") ?: ""
                    val description = doc.getString("description") ?: ""
                    val articleUrl = doc.getString("articleUrl") ?: ""
                    val source = doc.getString("source") ?: ""

                    if (title.isNotEmpty()) {
                        Article(
                            id = id,
                            title = title,
                            author = author,
                            category = category,
                            content = content,
                            imageName = imageName,
                            date = date,
                            description = description,
                            articleUrl = articleUrl,
                            source = source
                        )
                    } else null
                }

                scope.launch(Dispatchers.IO) {
                    if (articles.isNotEmpty()) {
                        articlesDao.deleteAllArticles()
                        articlesDao.insertAllArticles(articles)
                        Log.d("ArticleRepo", "Real-time article sync: ${articles.size} articles updated")
                    }
                }
            }
        }
    }

    /**
     * Fetches articles from Firestore and updates the local Room database.
     */
    suspend fun syncArticlesFromCloud() {
        try {
            val snapshot = db.collection("articles").get().await()
            val articles = snapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val title = doc.getString("title") ?: ""
                val author = doc.getString("author") ?: ""
                val category = doc.getString("category") ?: ""
                val content = doc.getString("content") ?: ""
                val imageName = doc.getString("imageName") ?: ""
                val date = doc.getString("date") ?: ""
                val description = doc.getString("description") ?: ""
                val articleUrl = doc.getString("articleUrl") ?: ""
                val source = doc.getString("source") ?: ""

                if (title.isNotEmpty()) {
                    Article(
                        id = id,
                        title = title,
                        author = author,
                        category = category,
                        content = content,
                        imageName = imageName,
                        date = date,
                        description = description,
                        articleUrl = articleUrl,
                        source = source
                    )
                } else null
            }

            if (articles.isNotEmpty()) {
                articlesDao.insertAllArticles(articles)
                Log.d("ArticleRepo", "Synced ${articles.size} articles from Firestore")
            }
        } catch (e: Exception) {
            Log.e("ArticleRepo", "Failed to sync articles", e)
        }
    }

    suspend fun insert(article: Article) {
        articlesDao.insertArticle(article)
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
