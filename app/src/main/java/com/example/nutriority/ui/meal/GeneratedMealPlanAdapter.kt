package com.example.nutriority.ui.meal

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutriority.R
import com.example.nutriority.data.model.Meal

// Sealed interface for our list items to create a type-safe list
sealed class MealListItem {
    abstract val id: String

    data class HeaderItem(val dateText: String) : MealListItem() {
        override val id: String = dateText
    }

    data class MealItem(val meal: Meal, val isToday: Boolean) : MealListItem() {
        // Create a unique ID for each meal item for DiffUtil to work correctly
        override val id: String = meal.name + meal.time
    }
}

private const val TYPE_HEADER = 0
private const val TYPE_MEAL = 1

class GeneratedMealPlanAdapter(
    private val onMealClick: (Meal) -> Unit
) : ListAdapter<MealListItem, RecyclerView.ViewHolder>(MealDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MealListItem.HeaderItem -> TYPE_HEADER
            is MealListItem.MealItem -> TYPE_MEAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_MEAL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal_details, parent, false)
                MealViewHolder(view, onMealClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MealListItem.HeaderItem -> (holder as HeaderViewHolder).bind(item)
            is MealListItem.MealItem -> (holder as MealViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val header: TextView = itemView.findViewById(R.id.date_header)

        fun bind(item: MealListItem.HeaderItem) {
            header.text = item.dateText
        }
    }

    class MealViewHolder(itemView: View, private val onMealClick: (Meal) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val mealImage: ImageView = itemView.findViewById(R.id.meal_image)
        private val mealTime: TextView = itemView.findViewById(R.id.meal_time)
        private val mealName: TextView = itemView.findViewById(R.id.meal_name)

        fun bind(item: MealListItem.MealItem) {
            val meal = item.meal
            mealName.text = meal.name
            mealTime.text = meal.time
            Glide.with(itemView.context)
                .load(meal.imageName)
                .placeholder(R.mipmap.ic_launcher)
                .into(mealImage)

            // Safely get and mutate the background drawable to change its color
            val mealTimeDrawable = mealTime.background.mutate() as? GradientDrawable
            mealTimeDrawable?.let { drawable ->
                val color = when (meal.time) {
                    "Breakfast" -> Color.parseColor("#537770")
                    "Lunch"     -> Color.parseColor("#c27d36")
                    "Dinner"    -> Color.parseColor("#416491")
                    else        -> Color.parseColor("#888888") // Default Gray
                }

                drawable.setColor(color) // Vibrant color for today
                mealName.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_dark))
            }

            itemView.setOnClickListener {
                onMealClick(meal)
            }
        }
    }

    // DiffUtil callback to efficiently update the list
    class MealDiffCallback : DiffUtil.ItemCallback<MealListItem>() {
        override fun areItemsTheSame(oldItem: MealListItem, newItem: MealListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MealListItem, newItem: MealListItem): Boolean {
            return oldItem == newItem
        }
    }
}
