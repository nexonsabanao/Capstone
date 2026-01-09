package com.example.nutriority.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nutriority.data.model.Article
import com.example.nutriority.databinding.ActivityArticleDetailBinding
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding
    private var currentArticle: Article? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        // Ensure the back button is visible and working
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Get article data from intent
        val articleJson = intent.getStringExtra("article_json")
        if (articleJson != null) {
            currentArticle = Gson().fromJson(articleJson, Article::class.java)
            displayArticleDetails()
        }
    }

    private fun displayArticleDetails() {
        currentArticle?.let { article ->
            binding.collapsingToolbar.title = article.title
            binding.articleTitle.text = article.title
            binding.articleAuthor.text = article.author
            binding.articleReadingTime.text = article.readingTime
            binding.articleCategory.text = article.category
            
            val resId = resources.getIdentifier(article.imageName, "drawable", packageName)
            if (resId != 0) {
                binding.articleImage.setImageResource(resId)
            }
        }
    }
}
