package com.example.nutriority.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.data.model.Meal
import com.example.nutriority.databinding.ItemPreviewMealCardBinding

class MealAdapter(private val onItemClick: (Meal) -> Unit) : ListAdapter<Meal, MealAdapter.MealViewHolder>(MealDiffCallback()) {

    class MealViewHolder(private val binding: ItemPreviewMealCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(meal: Meal, onItemClick: (Meal) -> Unit) {
            binding.mealName.text = meal.name
            // Format the integer calories into a user-friendly string
            binding.mealCalories.text = "${meal.calories} kcal"

            if (meal.imageResId != 0) {
                binding.mealImage.setImageResource(meal.imageResId)
            }

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
