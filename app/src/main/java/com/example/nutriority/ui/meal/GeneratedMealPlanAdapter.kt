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

sealed class MealListItem {
    abstract val id: String

    data class HeaderItem(val dateText: String, val dayIndex: Int) : MealListItem() {
        override val id: String = dateText
    }

    data class MealItem(val meal: Meal, val dayIndex: Int) : MealListItem() {
        override val id: String = meal.name + meal.mealTime + dayIndex
    }
}

private const val TYPE_HEADER = 0
private const val TYPE_MEAL = 1

class GeneratedMealPlanAdapter(
    private val onMealClick: (Meal) -> Unit,
    private val onSwapClick: (Meal, Int) -> Unit
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
                MealViewHolder(view, onMealClick, onSwapClick)
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

    class MealViewHolder(
        itemView: View, 
        private val onMealClick: (Meal) -> Unit,
        private val onSwapClick: (Meal, Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val mealImage: ImageView = itemView.findViewById(R.id.meal_image)
        private val mealTime: TextView = itemView.findViewById(R.id.meal_time)
        private val mealName: TextView = itemView.findViewById(R.id.meal_name)
        private val swapButton: ImageView = itemView.findViewById(R.id.reorder_button)

        fun bind(item: MealListItem.MealItem) {
            val meal = item.meal
            mealName.text = meal.name
            mealTime.text = meal.mealTime
            Glide.with(itemView.context)
                .load(meal.imageName)
                .placeholder(R.drawable.bg_meal_placeholder)
                .into(mealImage)

            val mealTimeDrawable = mealTime.background.mutate() as? GradientDrawable
            mealTimeDrawable?.let { drawable ->
                val color = when (meal.mealTime) {
                    "Breakfast" -> Color.parseColor("#537770")
                    "Lunch"     -> Color.parseColor("#c27d36")
                    "Dinner"    -> Color.parseColor("#416491")
                    else        -> Color.parseColor("#888888")
                }
                drawable.setColor(color)
                mealName.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_dark))
            }

            itemView.setOnClickListener { onMealClick(meal) }
            swapButton.setOnClickListener { onSwapClick(meal, item.dayIndex) }
        }
    }

    class MealDiffCallback : DiffUtil.ItemCallback<MealListItem>() {
        override fun areItemsTheSame(oldItem: MealListItem, newItem: MealListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MealListItem, newItem: MealListItem): Boolean {
            return oldItem == newItem
        }
    }
}
