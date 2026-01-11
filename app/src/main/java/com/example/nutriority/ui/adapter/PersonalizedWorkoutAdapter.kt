package com.example.nutriority.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.R
import com.example.nutriority.databinding.ItemPersonalizedWorkoutDayBinding
import com.example.nutriority.databinding.ItemRestartWorkoutBinding
import com.example.nutriority.planner.WorkoutSession

enum class DayStatus {
    LOCKED,
    ACTIVE,
    COMPLETED
}

class PersonalizedWorkoutAdapter(
    private var workoutSessions: List<WorkoutSession>,
    private var lastCompletedDay: Int,
    private val onStartWorkoutClicked: (dayIndex: Int) -> Unit,
    private val onRestartWorkoutClicked: () -> Unit,
    private val onWorkoutClicked: (workoutId: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_WORKOUT = 0
        private const val VIEW_TYPE_RESTART = 1
    }

    private var allWorkoutsCompleted = lastCompletedDay >= workoutSessions.size

    fun updateData(newSessions: List<WorkoutSession>, newLastCompletedDay: Int) {
        this.workoutSessions = newSessions
        this.lastCompletedDay = newLastCompletedDay
        this.allWorkoutsCompleted = lastCompletedDay >= workoutSessions.size
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (allWorkoutsCompleted && position == workoutSessions.size) {
            VIEW_TYPE_RESTART
        } else {
            VIEW_TYPE_WORKOUT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_WORKOUT -> {
                val binding = ItemPersonalizedWorkoutDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                WorkoutDayViewHolder(binding)
            }
            VIEW_TYPE_RESTART -> {
                val binding = ItemRestartWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                RestartButtonViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_WORKOUT -> {
                val workoutHolder = holder as WorkoutDayViewHolder
                val session = workoutSessions[position]
                val status = when {
                    position < lastCompletedDay -> DayStatus.COMPLETED
                    position == lastCompletedDay -> DayStatus.ACTIVE
                    else -> DayStatus.LOCKED
                }
                workoutHolder.bind(session, status)
            }
            VIEW_TYPE_RESTART -> {
                val restartHolder = holder as RestartButtonViewHolder
                restartHolder.bind()
            }
        }
    }

    override fun getItemCount(): Int {
        return if (allWorkoutsCompleted) {
            workoutSessions.size + 1
        } else {
            workoutSessions.size
        }
    }

    inner class WorkoutDayViewHolder(private val binding: ItemPersonalizedWorkoutDayBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < workoutSessions.size) {
                    val session = workoutSessions[position]
                    val id = session.unifiedWorkoutId
                    if (id != null && id > 0) {
                        onWorkoutClicked(id)
                    }
                }
            }
        }

        fun bind(session: WorkoutSession, status: DayStatus) {
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
                    binding.btnStart.setOnClickListener { onStartWorkoutClicked(bindingAdapterPosition) }

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

    inner class RestartButtonViewHolder(private val binding: ItemRestartWorkoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.btnRestartWorkout.setOnClickListener {
                onRestartWorkoutClicked()
            }
        }
    }
}
