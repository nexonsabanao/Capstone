package com.example.nutriority.ui.meal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutriority.R
import com.example.nutriority.data.model.Meal

private const val TYPE_HEADER = 0
private const val TYPE_MEAL = 1

class GeneratedMealPlanAdapter(private val data: List<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is String -> TYPE_HEADER
            is Meal -> TYPE_MEAL
            else -> throw IllegalArgumentException("Invalid type of data at position $position")
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
                MealViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.header.text = data[position] as String
            }
            is MealViewHolder -> {
                val meal = data[position] as Meal
                holder.mealName.text = meal.name
                holder.mealTime.text = meal.time
                Glide.with(holder.itemView.context)
                    .load(meal.imageName)
                    .placeholder(R.mipmap.ic_launcher)
                    .into(holder.mealImage)
            }
        }
    }

    override fun getItemCount(): Int = data.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val header: TextView = itemView.findViewById(R.id.date_header)
    }

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mealImage: ImageView = itemView.findViewById(R.id.meal_image)
        val mealTime: TextView = itemView.findViewById(R.id.meal_time)
        val mealName: TextView = itemView.findViewById(R.id.meal_name)
    }
}