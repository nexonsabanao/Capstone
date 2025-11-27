package com.example.nutriority.Models.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
// 1. IMPORT ListAdapter and DiffUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.Models.Article
import com.example.nutriority.databinding.ItemArticlePreviewBinding

// 2. CHANGE to extend ListAdapter and provide the DiffCallback
class ArticleAdapter : ListAdapter<Article, ArticleAdapter.ArticleViewHolder>(ArticleDiffCallback()) {

    // The ViewHolder class remains exactly the same.
    inner class ArticleViewHolder(val binding: ItemArticlePreviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemArticlePreviewBinding.inflate(inflater, parent, false)
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        // 3. USE getItem(position), which is provided by ListAdapter
        val currentArticle = getItem(position)

        holder.binding.apply {
            articleTitle.text = currentArticle.title
            articleAuthor.text = currentArticle.author
            articleImage.setImageResource(currentArticle.imageResId)
            articleReadingTime.text = currentArticle.readingTime
            articleCategory.text = currentArticle.category
        }
    }

    // 4. REMOVE getItemCount() and updateData().
    // ListAdapter manages these functions internally via submitList().

    // 5. ADD the required DiffUtil.ItemCallback class.
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
