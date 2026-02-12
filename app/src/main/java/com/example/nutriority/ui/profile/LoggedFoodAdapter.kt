package com.example.nutriority.ui.profile

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutriority.R
import com.example.nutriority.data.model.DailyMealLog
import java.io.File

class LoggedFoodAdapter(
    private val onDeleteClick: (DailyMealLog) -> Unit
) : ListAdapter<DailyMealLog, LoggedFoodAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_logged_food, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = getItem(position)
        holder.bind(log, onDeleteClick)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val mealTime: TextView = view.findViewById(R.id.tv_meal_time_label)
        private val foodImage: ImageView = view.findViewById(R.id.iv_food_image)
        private val foodName: TextView = view.findViewById(R.id.tv_food_name)
        private val protein: TextView = view.findViewById(R.id.tv_protein)
        private val fats: TextView = view.findViewById(R.id.tv_fats)
        private val carbs: TextView = view.findViewById(R.id.tv_carbs)
        private val calories: TextView = view.findViewById(R.id.tv_calories)
        private val deleteBtn: ImageView = view.findViewById(R.id.btn_delete)

        fun bind(log: DailyMealLog, onDelete: (DailyMealLog) -> Unit) {
            foodName.text = log.name
            protein.text = "${log.protein} g"
            fats.text = "${log.fats} g"
            carbs.text = "${log.carbs} g"
            calories.text = "${log.calories} kcal"
            mealTime.text = log.mealTime

            val context = itemView.context
            val requestBuilder = Glide.with(context).asDrawable().centerCrop()

            when {
                log.imageName.startsWith("/") -> {
                    requestBuilder.load(File(log.imageName))
                }
                log.imageName.startsWith("http") -> {
                    requestBuilder.load(log.imageName)
                }
                else -> {
                    val resId = context.resources.getIdentifier(log.imageName, "drawable", context.packageName)
                    if (resId != 0) {
                        requestBuilder.load(resId)
                    } else {
                        requestBuilder.load(R.drawable.bg_meal_placeholder)
                    }
                }
            }

            requestBuilder
                .placeholder(R.drawable.bg_meal_placeholder)
                .error(R.drawable.bg_meal_placeholder)
                .into(foodImage)

            val timeColor = when (log.mealTime.lowercase()) {
                "breakfast" -> Color.parseColor("#EBB861")
                "lunch" -> Color.parseColor("#F2994A")
                "dinner" -> Color.parseColor("#416491")
                else -> Color.parseColor("#888888")
            }
            val drawable = mealTime.background.mutate() as? GradientDrawable
            drawable?.setColor(timeColor)

            deleteBtn.setOnClickListener { onDelete(log) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DailyMealLog>() {
        override fun areItemsTheSame(oldItem: DailyMealLog, newItem: DailyMealLog): Boolean {
            // Using a combination of stable fields instead of auto-generated ID
            return oldItem.mealId == newItem.mealId && 
                   oldItem.name == newItem.name && 
                   oldItem.date == newItem.date
        }
        
        override fun areContentsTheSame(oldItem: DailyMealLog, newItem: DailyMealLog): Boolean {
            return oldItem == newItem
        }
    }
}
