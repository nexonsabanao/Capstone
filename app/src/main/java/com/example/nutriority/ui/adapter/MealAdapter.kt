package com.example.nutriority.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutriority.R
import com.example.nutriority.data.model.Meal
import com.example.nutriority.databinding.ItemPreviewMealCardBinding
import java.io.File

class MealAdapter(private val onItemClick: (Meal) -> Unit) : ListAdapter<Meal, MealAdapter.MealViewHolder>(MealDiffCallback()) {

    class MealViewHolder(private val binding: ItemPreviewMealCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(meal: Meal, onItemClick: (Meal) -> Unit) {
            binding.mealName.text = meal.name
            binding.mealCalories.text = "${meal.calories} kcal"

            val context = binding.mealImage.context
            
            // Check if the imageName refers to a local WebP file path, a URL, or a drawable resource
            val requestBuilder = Glide.with(context).asDrawable().centerCrop()
            
            when {
                // If it's a local file path (from manual logging)
                meal.imageName.startsWith("/") -> {
                    requestBuilder.load(File(meal.imageName))
                }
                // If it's a URL (from cloud sync)
                meal.imageName.startsWith("http") -> {
                    requestBuilder.load(meal.imageName)
                }
                // If it's a drawable resource ID (from library)
                meal.imageResId != 0 -> {
                    requestBuilder.load(meal.imageResId)
                }
                // Fallback to placeholder
                else -> {
                    requestBuilder.load(R.drawable.bg_meal_placeholder)
                }
            }
            
            requestBuilder
                .placeholder(R.drawable.bg_meal_placeholder)
                .error(R.drawable.bg_meal_placeholder)
                .into(binding.mealImage)

            binding.root.setOnClickListener {
                onItemClick(meal)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemPreviewMealCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val currentMeal = getItem(position)
        holder.bind(currentMeal, onItemClick)
    }

    class MealDiffCallback : DiffUtil.ItemCallback<Meal>() {
        override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean {
            return oldItem == newItem
        }
    }
}
