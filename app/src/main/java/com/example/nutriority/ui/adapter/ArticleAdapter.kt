package com.example.nutriority.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutriority.R
import com.example.nutriority.data.model.Article
import com.example.nutriority.databinding.ItemArticlePreviewBinding

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
            articleAuthor.text = currentArticle.author
            articleReadingTime.text = currentArticle.readingTime
            articleCategory.text = currentArticle.category

            // Use Glide for efficient article preview image loading
            if (currentArticle.imageResId != 0) {
                Glide.with(articleImage.context)
                    .load(currentArticle.imageResId)
                    .centerCrop()
                    .placeholder(R.drawable.img_balanced_diet)
                    .into(articleImage)
            } else {
                articleImage.setImageResource(R.drawable.img_balanced_diet)
            }

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
