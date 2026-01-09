package com.example.nutriority.ui.workout

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.example.nutriority.R
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.databinding.ActivityWorkoutDetailBinding
import com.example.nutriority.databinding.DialogEditWorkoutBinding
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.example.nutriority.ui.adapter.SelectableExerciseAdapter
import com.example.nutriority.ui.adapter.WorkoutItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutDetailBinding
    private val viewModel: WorkoutDetailViewModel by viewModels()
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var currentWorkoutId: Int = -1
    private var lastToastTime: Long = 0
    private val TOAST_DURATION_MS = 800L
    private val TOAST_COOLDOWN_MS = 900L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.collapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT)

        currentWorkoutId = intent.getIntExtra("workout_id", -1)
        if (currentWorkoutId != -1) {
            viewModel.getWorkoutById(currentWorkoutId)
        }

        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.addExerciseButton.setOnClickListener {
            showEditWorkoutDialog()
        }
        
        binding.switchIncludeWarmupCooldown.setOnCheckedChangeListener { _, isChecked ->
            viewModel.workout.value?.let { workout ->
                updateDisplayList(workout, isChecked)
            }
        }
    }
    
    private fun showThrottledToast(message: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastToastTime > TOAST_COOLDOWN_MS) {
            val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
            toast.show()
            
            // Fast-dismiss logic: cancel the toast after 1 second
            lifecycleScope.launch {
                delay(TOAST_DURATION_MS)
                toast.cancel()
            }
            
            lastToastTime = currentTime
        }
    }

    private fun showEditWorkoutDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        val dialogBinding = DialogEditWorkoutBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)

        val selectableAdapter = SelectableExerciseAdapter { _, _ -> }

        dialogBinding.rvSelectExercises.apply {
            layoutManager = LinearLayoutManager(this@WorkoutDetailActivity)
            adapter = selectableAdapter
        }

        lifecycleScope.launch {
            // Wait for workout data
            val workoutWithExercises = viewModel.workout.filterNotNull().first()
            // Wait for all exercises data
            val allExercises = viewModel.getAllExercises().filter { it.isNotEmpty() }.first()
            
            val workoutName = workoutWithExercises.workout.name
            // IMPROVED SYSTEM WORKOUT DETECTION: Check ID range (1-25 are default) or name keywords
            val isSystemWorkout = workoutWithExercises.workout.id <= 25 || 
                listOf("Arm", "Abs", "Chest", "Leg", "Shoulder", "Back", "Full Body", "Lower Body")
                    .any { workoutName.contains(it, ignoreCase = true) }
            
            // CONFIGURE UI
            if (isSystemWorkout) {
                dialogBinding.tvSystemWorkoutName.text = workoutName
                dialogBinding.tvSystemWorkoutName.visibility = View.VISIBLE
                dialogBinding.workoutNameLayout.visibility = View.GONE
                dialogBinding.btnReset.visibility = View.VISIBLE
            } else {
                dialogBinding.etWorkoutName.setText(workoutName)
                dialogBinding.tvSystemWorkoutName.visibility = View.GONE
                dialogBinding.workoutNameLayout.visibility = View.VISIBLE
                dialogBinding.btnReset.visibility = View.GONE
            }
            
            // Capture the state when the dialog was opened for the Reset button
            val initialExercises = workoutWithExercises.exercises
            selectableAdapter.setData(allExercises, initialExercises)

            dialogBinding.categoryChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
                val category = when (checkedIds.firstOrNull()) {
                    R.id.chip_warmup -> "Warm-up"
                    R.id.chip_cooldown -> "Cool-down"
                    else -> "Exercise"
                }
                selectableAdapter.setFilter(category)
            }

            dialogBinding.btnReset.setOnClickListener {
                // BUG FIX: Reset to the exercises that were active when the dialog opened,
                // rather than filtering the grouped 'allExercises' list which loses associations.
                selectableAdapter.setData(allExercises, initialExercises)
                
                // FIX: Only show toast if it's not currently on screen (throttled by time)
                showThrottledToast("Reset to default exercises")
            }

            dialogBinding.btnSave.setOnClickListener {
                val finalName = if (isSystemWorkout) workoutName else dialogBinding.etWorkoutName.text.toString()
                if (finalName.isBlank()) {
                    showThrottledToast("Please enter a workout name")
                    return@setOnClickListener
                }

                val finalSelectedExercises = selectableAdapter.getSelectedExercises()
                viewModel.updateWorkout(workoutWithExercises.workout.copy(name = finalName), finalSelectedExercises)
                dialog.dismiss()
            }

            dialogBinding.toolbar.setNavigationOnClickListener { dialog.dismiss() }

            // GENIUS FIX: DATA IS LOADED, REVEAL CONTENT AND HIDE PROGRESS
            dialogBinding.loadingProgress.visibility = View.GONE
            dialogBinding.contentLayout.visibility = View.VISIBLE
        }

        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseAdapter(
            onItemClick = { exercise, position, _ ->
                // BUG FIX: Correctly calculate position based on ExerciseItems ONLY
                val currentList = exerciseAdapter.currentList
                val exerciseItems = currentList.filterIsInstance<WorkoutItem.ExerciseItem>()
                val totalExercises = exerciseItems.size
                
                // Find index of this specific exercise object in the filtered list
                val exercisePos = exerciseItems.indexOfFirst { it.exercise.id == exercise.id } + 1
                
                val intent = Intent(this, ExerciseDetailActivity::class.java).apply {
                    putExtra("exercise_id", exercise.id)
                    putExtra("exercise_position", exercisePos)
                    putExtra("total_exercises", totalExercises)
                }
                startActivity(intent)
            },
            onListUpdated = { updatedList ->
                viewModel.updateExercises(updatedList)
            },
            onDragStart = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            },
            showDragHandle = true
        )

        binding.exercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@WorkoutDetailActivity)
            adapter = exerciseAdapter
        }

        val callback = SimpleItemTouchHelperCallback(exerciseAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.exercisesRecyclerView)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.workout.collect { workoutWithExercises ->
                workoutWithExercises?.let { workout ->
                    binding.collapsingToolbar.title = workout.workout.name
                    binding.workoutTitle.text = workout.workout.name
                    
                    updateDisplayList(workout, binding.switchIncludeWarmupCooldown.isChecked)
                }
            }
        }
    }

    private fun updateDisplayList(workout: WorkoutWithExercises, includeAll: Boolean) {
        TransitionManager.beginDelayedTransition(binding.contentContainer, AutoTransition().apply {
            duration = 300
        })

        val displayList = mutableListOf<WorkoutItem>()
        
        val processedExercises = workout.exercises.map { ex ->
            if (ex.category.equals("Warm-up", ignoreCase = true) || ex.category.equals("Cool-down", ignoreCase = true)) {
                ex.copy(
                    sets = if (ex.sets <= 0) 1 else ex.sets,
                    duration = if (ex.duration.isBlank()) "30s" else ex.duration
                ).apply { imageResId = ex.imageResId }
            } else {
                ex
            }
        }.sortedBy { it.order }

        val warmup = processedExercises.filter { it.category.equals("Warm-up", ignoreCase = true) }
        val cooldown = processedExercises.filter { it.category.equals("Cool-down", ignoreCase = true) }
        val main = processedExercises.filter { 
            !it.category.equals("Warm-up", ignoreCase = true) && 
            !it.category.equals("Cool-down", ignoreCase = true) 
        }

        binding.workoutExerciseCount.text = main.size.toString()

        if (includeAll) {
            if (warmup.isNotEmpty()) {
                displayList.add(WorkoutItem.DividerItem("Warm-up"))
                displayList.addAll(warmup.map { WorkoutItem.ExerciseItem(it) })
            }
            
            displayList.add(WorkoutItem.DividerItem("Exercises"))
            displayList.addAll(main.map { WorkoutItem.ExerciseItem(it) })
            
            if (cooldown.isNotEmpty()) {
                displayList.add(WorkoutItem.DividerItem("Cool-down"))
                displayList.addAll(cooldown.map { WorkoutItem.ExerciseItem(it) })
            }
        } else {
            displayList.addAll(main.map { WorkoutItem.ExerciseItem(it) })
        }

        val totalSeconds = processedExercises.filter { 
            includeAll || (!it.category.equals("Warm-up", ignoreCase = true) && !it.category.equals("Cool-down", ignoreCase = true))
        }.sumOf { exercise ->
            val durationStr = exercise.duration.lowercase().trim()
            if (durationStr.contains("s") || durationStr.contains(":")) {
                val secondsPerSet = if (durationStr.contains(":")) {
                    val parts = durationStr.split(":")
                    (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
                } else {
                    durationStr.filter { it.isDigit() }.toIntOrNull() ?: 30
                }
                exercise.sets * secondsPerSet
            } else {
                val repsList = exercise.reps.split(",").mapNotNull { it.trim().toIntOrNull() }
                val totalReps = if (repsList.isNotEmpty()) repsList.sum() else exercise.sets * 10
                (totalReps * 3) + (exercise.sets * 45)
            }
        }
        
        binding.workoutDuration.text = "${Math.ceil(totalSeconds / 60.0).toInt()} mins"
        exerciseAdapter.submitList(displayList)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.exercisesRecyclerView.adapter = null
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
