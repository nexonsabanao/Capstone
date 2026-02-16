package com.example.nutriority.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutriority.R
import com.example.nutriority.data.model.Meal
import com.example.nutriority.databinding.ItemAllMealCardBinding
import com.example.nutriority.databinding.ItemMealSectionHeaderBinding
import java.io.File

sealed class AllMealItem {
    data class Header(val title: String) : AllMealItem()
    data class MealItem(val meal: Meal) : AllMealItem()
}

class AllMealAdapter(private val onItemClick: (Meal) -> Unit) : ListAdapter<AllMealItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_MEAL = 1
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is AllMealItem.Header -> TYPE_HEADER
        is AllMealItem.MealItem -> TYPE_MEAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemMealSectionHeaderBinding.inflate(inflater, parent, false))
        } else {
            MealViewHolder(ItemAllMealCardBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is HeaderViewHolder && item is AllMealItem.Header) {
            holder.bind(item.title)
        } else if (holder is MealViewHolder && item is AllMealItem.MealItem) {
            holder.bind(item.meal, onItemClick)
        }
    }

    class HeaderViewHolder(private val binding: ItemMealSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) { binding.tvSectionHeader.text = title }
    }

    class MealViewHolder(private val binding: ItemAllMealCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(meal: Meal, onItemClick: (Meal) -> Unit) {
            binding.mealName.text = meal.name
            binding.mealCalories.text = "${meal.calories} kcal"
            
            // Remove the meal_time TextView as per redesign request for dividers
            // binding.meal_time.text = meal.mealTime 

            val context = binding.mealImage.context
            val requestBuilder = Glide.with(context).asDrawable().centerCrop()
            
            when {
                meal.imageName.startsWith("/") -> requestBuilder.load(File(meal.imageName))
                meal.imageName.startsWith("http") -> requestBuilder.load(meal.imageName)
                meal.imageResId != 0 -> requestBuilder.load(meal.imageResId)
                else -> requestBuilder.load(R.drawable.bg_meal_placeholder)
            }
            
            requestBuilder.placeholder(R.drawable.bg_meal_placeholder).into(binding.mealImage)
            binding.root.setOnClickListener { onItemClick(meal) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AllMealItem>() {
        override fun areItemsTheSame(oldItem: AllMealItem, newItem: AllMealItem): Boolean {
            return if (oldItem is AllMealItem.Header && newItem is AllMealItem.Header) oldItem.title == newItem.title
            else if (oldItem is AllMealItem.MealItem && newItem is AllMealItem.MealItem) oldItem.meal.id == newItem.meal.id
            else false
        }
        override fun areContentsTheSame(oldItem: AllMealItem, newItem: AllMealItem): Boolean = oldItem == newItem
    }

    fun setupSpanManager(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager
        layoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (getItemViewType(position) == TYPE_HEADER) 2 else 1
            }
        }
    }
}
