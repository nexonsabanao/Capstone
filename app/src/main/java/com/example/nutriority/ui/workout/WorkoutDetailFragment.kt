package com.example.nutriority.ui.workout

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.databinding.FragmentWorkoutDetailBinding
import com.example.nutriority.databinding.DialogEditWorkoutBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.example.nutriority.ui.adapter.SelectableExerciseAdapter
import com.example.nutriority.ui.adapter.WorkoutItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!
    
    // Use ActivityViewModel for shared navigation state
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val viewModel: WorkoutDetailViewModel by activityViewModels()
    
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fix: Use NavigationViewModel for back navigation
        binding.toolbar.setNavigationOnClickListener {
            navigationViewModel.goBack()
        }

        binding.collapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT)
        binding.collapsingToolbar.setCollapsedTitleTextColor(Color.BLACK)

        setupRecyclerView()
        observeNavigationData()
        observeViewModel()
        setupClickListeners()
    }

    private fun observeNavigationData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationViewModel.selectedWorkoutId.collect { workoutId ->
                    if (workoutId != -1) {
                        viewModel.getWorkoutById(workoutId)
                        // Reset scroll
                        binding.nestedScrollView.scrollTo(0, 0)
                        binding.appBarLayout.setExpanded(true)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.addExerciseButton.setOnClickListener {
            showEditWorkoutDialog()
        }

        binding.switchIncludeWarmupCooldown.setOnCheckedChangeListener { _, isChecked ->
            viewModel.workout.value?.let { workout ->
                updateDisplayList(workout, isChecked)
                viewModel.updateWorkoutPreference(isChecked)
            }
        }
        
        binding.startButton.setOnClickListener {
            val exercises = exerciseAdapter.currentList.filterIsInstance<WorkoutItem.ExerciseItem>()
            if (exercises.isNotEmpty()) {
                // Navigation to Exercise Detail can remain as standard navigate for now 
                // as it's a deep linear flow, but we'll monitor performance.
            }
        }
    }

    private fun showEditWorkoutDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
        val dialogBinding = DialogEditWorkoutBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val selectableAdapter = SelectableExerciseAdapter { _, _ -> }

        dialogBinding.rvSelectExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectableAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val workoutWithExercises = viewModel.workout.filterNotNull().first()
            val allExercises = viewModel.getAllExercises().filter { it.isNotEmpty() }.first()

            val workoutName = workoutWithExercises.workout.name
            val isSystemWorkout = workoutWithExercises.workout.id <= 25 ||
                    listOf("Arm", "Abs", "Chest", "Leg", "Shoulder", "Back", "Full Body", "Lower Body")
                        .any { workoutName.contains(it, ignoreCase = true) }

            if (isSystemWorkout) {
                dialogBinding.workoutNameLayout.visibility = View.GONE
                dialogBinding.btnReset.visibility = View.VISIBLE
            } else {
                dialogBinding.etWorkoutName.setText(workoutName)
                dialogBinding.workoutNameLayout.visibility = View.VISIBLE
                dialogBinding.btnReset.visibility = View.GONE
            }

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
                viewLifecycleOwner.lifecycleScope.launch {
                    val defaultExercises = viewModel.getDefaultExercisesFromAssets(workoutName)
                    if (defaultExercises.isNotEmpty()) {
                        selectableAdapter.setData(allExercises, defaultExercises)
                    } else {
                        selectableAdapter.setData(allExercises, initialExercises)
                    }
                }
            }

            dialogBinding.btnSave.setOnClickListener {
                val finalName = if (isSystemWorkout) workoutName else dialogBinding.etWorkoutName.text.toString()
                if (finalName.isBlank()) {
                    return@setOnClickListener
                }

                val finalSelectedExercises = selectableAdapter.getSelectedExercises()
                viewModel.updateWorkout(workoutWithExercises.workout.copy(name = finalName), finalSelectedExercises)
                dialog.dismiss()
            }

            dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

            dialogBinding.loadingProgress.visibility = View.GONE
            dialogBinding.contentLayout.visibility = View.VISIBLE
        }

        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseAdapter(
            onItemClick = { _, _, _ ->
                // Implementation for exercise detail navigation
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
            layoutManager = LinearLayoutManager(requireContext())
            if (adapter != exerciseAdapter) adapter = exerciseAdapter
        }

        val callback = SimpleItemTouchHelperCallback(exerciseAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.exercisesRecyclerView)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.workout.collect { workoutWithExercises ->
                    workoutWithExercises?.let { workout ->
                        binding.collapsingToolbar.title = workout.workout.name
                        binding.workoutTitle.text = workout.workout.name

                        if (binding.switchIncludeWarmupCooldown.isChecked != workout.workout.includeWarmupCooldown) {
                            binding.switchIncludeWarmupCooldown.isChecked = workout.workout.includeWarmupCooldown
                        }

                        updateDisplayList(workout, workout.workout.includeWarmupCooldown)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (isLoading) {
                        binding.exercisesRecyclerView.visibility = View.GONE
                        binding.loadingProgress.visibility = View.VISIBLE
                    } else {
                        binding.exercisesRecyclerView.visibility = View.VISIBLE
                        binding.loadingProgress.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun updateDisplayList(workout: WorkoutWithExercises, includeAll: Boolean) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
