package com.example.nutriority.models.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
// 1. IMPORT ListAdapter and DiffUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.models.Article
import com.example.nutriority.databinding.ItemArticlePreviewBinding

class ArticleAdapter : ListAdapter<Article, ArticleAdapter.ArticleViewHolder>(ArticleDiffCallback()) {

    inner class ArticleViewHolder(val binding: ItemArticlePreviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemArticlePreviewBinding.inflate(inflater, parent, false)
        return ArticleViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val currentArticle = getItem(position)

        holder.binding.apply {
            articleTitle.text = currentArticle.title
            articleAuthor.text = currentArticle.author
            val resId = if (currentArticle.imageResId != 0) {
                currentArticle.imageResId
            } else {
                val ctx = articleImage.context
                ctx.resources.getIdentifier(currentArticle.imageName, "drawable", ctx.packageName)
            }
            if (resId != 0) {
                articleImage.setImageResource(resId)
            }
            articleReadingTime.text = currentArticle.readingTime
            articleCategory.text = currentArticle.category
        }
    }

    class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            // Use a unique identifier, like the title or a potential ID field.
            return oldItem.title == newItem.title // Assuming title is unique for now
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            // The data class '==' implementation checks all properties, which is perfect.
            return oldItem == newItem
        }
    }
}
