package com.example.nutriority.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nutriority.models.Article
import kotlinx.coroutines.flow.Flow

@Dao // Marks the class as a Data Access Object for Room
interface ArticlesDao {

    /**
     * Inserts a single article into the database.
     * If an article with the same primary key already exists, it will be replaced.
     * @param article The Article object to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: Article)

    /**
     * Inserts a list of articles in a single transaction.
     * If any article already exists, it will be replaced.
     * @param articles The list of Article objects to insert.
     */
    // FIX: Renamed from insertAll to insertAllArticles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllArticles(articles: List<Article>)

    /**
     * Updates an existing article in the table.
     * @param article The Article object to update.
     */
    @Update
    suspend fun updateArticle(article: Article)

    /**
     * Deletes a specific article from the table.
     * @param article The Article object to delete.
     */
    @Delete
    suspend fun deleteArticle(article: Article)

    /**
     * Retrieves all articles from the 'articles' table, ordered by title.
     * It returns a Flow, allowing the UI to reactively observe data changes.
     * @return A Flow emitting a list of all Article objects.
     */
    @Query("SELECT * FROM articles ORDER BY title ASC")
    fun getAllArticles(): Flow<List<Article>>

    /**
     * Retrieves a specific article by its ID.
     * @param articleId The ID of the article to retrieve.
     * @return A Flow emitting the Article object, or null if not found.
     */
    @Query("SELECT * FROM articles WHERE id = :articleId")
    fun getArticleById(articleId: Int): Flow<Article?>

    /**
     * Deletes all entries from the 'articles' table.
     * Useful for clearing the cache before fetching new data.
     */
    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()
}
