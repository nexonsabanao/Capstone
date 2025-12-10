package com.example.nutriority.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.data.model.Article
import com.example.nutriority.databinding.ItemArticlePreviewBinding

class ArticleAdapter : ListAdapter<Article, ArticleAdapter.ArticleViewHolder>(ArticleDiffCallback()) {

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
            articleAuthor.text = currentArticle.author
            articleReadingTime.text = currentArticle.readingTime
            articleCategory.text = currentArticle.category

            // The adapter now relies on imageResId being pre-calculated.
            if (currentArticle.imageResId != 0) {
                articleImage.setImageResource(currentArticle.imageResId)
            }
        }
    }

    class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            // Use the stable, unique ID for comparison.
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            // The data class '==' implementation checks all properties, which is perfect.
            return oldItem == newItem
        }
    }
}
