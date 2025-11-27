package com.example.nutriority.models.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
// 1. IMPORT ListAdapter and DiffUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.models.Meal
import com.example.nutriority.databinding.ItemMealPreviewBinding

// 2. CHANGE RecyclerView.Adapter to ListAdapter
//    - Remove the constructor parameter.
//    - Pass a DiffUtil.ItemCallback to the ListAdapter constructor.
class MealAdapter : ListAdapter<Meal, MealAdapter.MealViewHolder>(MealDiffCallback()) {

    // The inner class stays the same.
    inner class MealViewHolder(private val binding: ItemMealPreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(meal: Meal) {
            binding.mealName.text = meal.name
            binding.mealCalories.text = meal.calories // Correctly gets the string
            binding.mealImage.setImageResource(meal.imageResId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealPreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MealViewHolder(binding)
    }

    // 3. CHANGE onBindViewHolder to use getItem(position)
    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        // getItem() is a built-in method from ListAdapter
        val currentMeal = getItem(position)
        holder.bind(currentMeal)
    }

    // 4. REMOVE getItemCount() and updateData()
    // ListAdapter manages the item count and data updates automatically.

    // 5. ADD a DiffUtil.ItemCallback class
    // This tells the ListAdapter how to check for differences between items.
    class MealDiffCallback : DiffUtil.ItemCallback<Meal>() {
        override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean {
            // Check if the items represent the same object (e.g., by their unique ID).
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean {
            // Check if the contents of the items are the same.
            // The data class '==' implementation handles this perfectly.
            return oldItem == newItem
        }
    }
}
