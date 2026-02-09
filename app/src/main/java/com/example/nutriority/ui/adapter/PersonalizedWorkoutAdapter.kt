package com.example.nutriority.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.R
import com.example.nutriority.databinding.ItemPersonalizedWorkoutDayBinding
import com.example.nutriority.planner.WorkoutSession

enum class DayStatus {
    LOCKED,
    ACTIVE,
    COMPLETED
}

class PersonalizedWorkoutAdapter(
    private var lastCompletedDay: Int,
    private val onStartWorkoutClicked: (dayIndex: Int) -> Unit,
    private val onWorkoutClicked: (workoutId: Int, dayIndex: Int) -> Unit
) : ListAdapter<WorkoutSession, PersonalizedWorkoutAdapter.WorkoutDayViewHolder>(WorkoutSessionDiffCallback()) {

    fun updateLastCompletedDay(newLastCompletedDay: Int) {
        this.lastCompletedDay = newLastCompletedDay
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutDayViewHolder {
        val binding = ItemPersonalizedWorkoutDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutDayViewHolder, position: Int) {
        val session = getItem(position)
        // Note: The global day index is needed for status check.
        // We assume the list passed to submitList is the subset for the current week.
        // We'll need the global index to correctly determine status.
        val currentWeek = lastCompletedDay / 7
        val globalIndex = (currentWeek * 7) + position
        
        val status = when {
            globalIndex < lastCompletedDay -> DayStatus.COMPLETED
            globalIndex == lastCompletedDay -> DayStatus.ACTIVE
            else -> DayStatus.LOCKED
        }
        holder.bind(session, status, globalIndex)
    }

    inner class WorkoutDayViewHolder(private val binding: ItemPersonalizedWorkoutDayBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val session = getItem(position)
                    val currentWeek = lastCompletedDay / 7
                    val globalIndex = (currentWeek * 7) + position
                    val id = session.unifiedWorkoutId
                    if (id != null && id > 0) {
                        onWorkoutClicked(id, globalIndex)
                    }
                }
            }
        }

        fun bind(session: WorkoutSession, status: DayStatus, globalIndex: Int) {
            val context = binding.root.context
            binding.tvDayTitle.text = session.day
            
            binding.tvDayDetails.text = if (session.focus != "Rest Day") {
                "${session.durationMinutes} min · ${session.caloriesBurned} kcal"
            } else {
                session.description
            }

            binding.btnStart.text = "Start"

            when (status) {
                DayStatus.ACTIVE -> {
                    binding.dayCardContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_dark))
                    binding.timelineDot.setBackgroundResource(R.drawable.dot_active)
                    binding.tvDayTitle.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    binding.tvDayDetails.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    
                    binding.btnStart.visibility = View.VISIBLE
                    binding.btnStart.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green))
                    binding.btnStart.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    
                    binding.ivWorkoutImage.visibility = View.VISIBLE
                    binding.ivWorkoutImage.setImageResource(R.drawable.ic_play_arrow)
                    binding.ivWorkoutImage.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white))
                    binding.btnStart.setOnClickListener { onStartWorkoutClicked(globalIndex) }

                    if (session.focus == "Rest Day") {
                        binding.btnStart.text = "Complete Day"
                        binding.ivWorkoutImage.visibility = View.GONE
                    }
                }
                DayStatus.COMPLETED -> {
                    binding.dayCardContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                    binding.timelineDot.setBackgroundResource(R.drawable.dot_completed)
                    binding.tvDayTitle.setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
                    binding.tvDayDetails.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                    binding.btnStart.visibility = View.GONE
                    binding.ivWorkoutImage.visibility = View.VISIBLE
                    binding.ivWorkoutImage.setImageResource(R.drawable.ic_check_circle)
                    binding.ivWorkoutImage.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green))
                }
                DayStatus.LOCKED -> {
                    binding.dayCardContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                    binding.timelineDot.setBackgroundResource(R.drawable.dot_inactive)
                    binding.tvDayTitle.setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
                    binding.tvDayDetails.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                    binding.btnStart.visibility = View.GONE
                    binding.ivWorkoutImage.visibility = View.VISIBLE
                    binding.ivWorkoutImage.setImageResource(R.drawable.ic_lock)
                    binding.ivWorkoutImage.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.dark_gray))
                }
            }

            if (session.focus == "Rest Day" && status != DayStatus.COMPLETED) {
                binding.ivWorkoutImage.visibility = View.GONE
            }
        }
    }

    class WorkoutSessionDiffCallback : DiffUtil.ItemCallback<WorkoutSession>() {
        override fun areItemsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession): Boolean {
            return oldItem.day == newItem.day
        }

        override fun areContentsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession): Boolean {
            return oldItem == newItem
        }
    }
}
