package com.example.nutriority.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.nutriority.R
import com.example.nutriority.data.model.Article
import com.example.nutriority.databinding.ItemArticlePreviewBinding
import java.io.File

class ArticleAdapter(
    private val onArticleClick: (Article) -> Unit
) : ListAdapter<Article, ArticleAdapter.ArticleViewHolder>(ArticleDiffCallback()) {

    class ArticleViewHolder(val binding: ItemArticlePreviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemArticlePreviewBinding.inflate(inflater, parent, false)
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val currentArticle = getItem(position)

        holder.binding.apply {
            articleTitle.text = currentArticle.title
            articleAuthor.text = "by ${currentArticle.author}"
            articleCategory.text = currentArticle.category
            
            articleReadingTime.text = "Read Article"

            val context = articleImage.context
            val requestBuilder = Glide.with(context)
                .asDrawable()
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())

            when {
                // If it's a local WebP file path
                currentArticle.imageName.startsWith("/") -> {
                    requestBuilder.load(File(currentArticle.imageName))
                }
                // If it's a URL
                currentArticle.imageName.startsWith("http") -> {
                    requestBuilder.load(currentArticle.imageName)
                }
                // Fallback to placeholder
                else -> {
                    requestBuilder.load(R.drawable.bg_article_placeholder)
                }
            }

            requestBuilder
                .placeholder(R.drawable.bg_article_placeholder)
                .error(R.drawable.bg_article_placeholder)
                .into(articleImage)

            root.setOnClickListener {
                onArticleClick(currentArticle)
            }
        }
    }

    class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem == newItem
        }
    }
}
