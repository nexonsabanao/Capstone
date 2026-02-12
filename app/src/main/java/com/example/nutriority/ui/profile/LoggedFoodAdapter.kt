package com.example.nutriority.ui.profile

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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
            
            // Set initial placeholder state
            resetToPlaceholder()

            val hasValidImage = !log.imageName.isNullOrEmpty() && log.imageName != "bg_image_placeholder"

            if (hasValidImage) {
                val imageSource: Any? = when {
                    log.imageName.startsWith("/") -> File(log.imageName)
                    log.imageName.startsWith("http") -> log.imageName
                    else -> {
                        val resId = context.resources.getIdentifier(log.imageName, "drawable", context.packageName)
                        if (resId != 0) resId else null
                    }
                }

                if (imageSource != null) {
                    Glide.with(context)
                        .load(imageSource)
                        .centerCrop()
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                resetToPlaceholder()
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                foodImage.scaleType = ImageView.ScaleType.CENTER_CROP
                                foodImage.setPadding(0, 0, 0, 0)
                                foodImage.setBackgroundColor(Color.TRANSPARENT)
                                foodImage.imageTintList = null // Clear tint for real images
                                return false
                            }
                        })
                        .into(foodImage)
                }
            }

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

        private fun resetToPlaceholder() {
            foodImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
            foodImage.setPadding(8, 8, 8, 8)
            foodImage.setBackgroundColor(Color.parseColor("#EEEEEE"))
            foodImage.setImageResource(R.drawable.ic_award_meal_24)
            foodImage.imageTintList = ColorStateList.valueOf(Color.parseColor("#BDBDBD"))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DailyMealLog>() {
        override fun areItemsTheSame(oldItem: DailyMealLog, newItem: DailyMealLog): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: DailyMealLog, newItem: DailyMealLog): Boolean {
            return oldItem == newItem
        }
    }
}
