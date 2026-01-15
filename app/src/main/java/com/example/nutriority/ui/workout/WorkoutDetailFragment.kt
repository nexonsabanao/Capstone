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
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import com.example.nutriority.data.model.WorkoutWithExercises
import com.example.nutriority.databinding.FragmentWorkoutDetailBinding
import com.example.nutriority.databinding.DialogEditWorkoutBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.example.nutriority.ui.adapter.SelectableExerciseAdapter
import com.example.nutriority.ui.adapter.WorkoutItem
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!
    
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val viewModel: WorkoutDetailViewModel by activityViewModels()
    
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    
    private var isSettingInitialState = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            navigationViewModel.goBack()
        }

        binding.tvToolbarTitle.alpha = 0f
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)

        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener

            val percentage = abs(verticalOffset).toFloat() / totalScrollRange
            val startFadeAt = 0.8f
            if (percentage > startFadeAt) {
                val alphaProgress = (percentage - startFadeAt) / (1f - startFadeAt)
                val alphaInt = (alphaProgress * 255).toInt().coerceIn(0, 255)
                binding.toolbar.setBackgroundColor(Color.argb(alphaInt, 255, 255, 255))
                binding.tvToolbarTitle.alpha = alphaProgress
            } else {
                binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
                binding.tvToolbarTitle.alpha = 0f
            }
        })

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
            if (isSettingInitialState) return@setOnCheckedChangeListener
            viewModel.updateWorkoutPreference(isChecked)
        }
        
        binding.startButton.setOnClickListener {
            val items = exerciseAdapter.currentList.filterIsInstance<WorkoutItem.ExerciseItem>()
            if (items.isNotEmpty()) {
                val firstAssignment = items[0].detail.assignment
                navigationViewModel.navigateToExerciseDetail(
                    firstAssignment.workoutId,
                    firstAssignment.exerciseId,
                    1,
                    items.size
                )
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
            val isSystemWorkout = workoutWithExercises.workout.id <= 25

            if (isSystemWorkout) {
                dialogBinding.workoutNameLayout.visibility = View.GONE
                dialogBinding.btnReset.visibility = View.VISIBLE
            } else {
                dialogBinding.etWorkoutName.setText(workoutName)
                dialogBinding.workoutNameLayout.visibility = View.VISIBLE
                dialogBinding.btnReset.visibility = View.GONE
            }

            val selectedExercises = workoutWithExercises.exerciseAssignments.map { assignment ->
                assignment.exercise.copy(category = assignment.assignment.category)
            }
            selectableAdapter.setData(allExercises, selectedExercises)

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
                    val defaults = viewModel.getDefaultAssignmentsFromAssets(workoutWithExercises.workout.id, workoutName)
                    if (defaults.isNotEmpty()) {
                        val defaultExercises = defaults.mapNotNull { assignment ->
                            allExercises.find { it.id == assignment.exerciseId }?.copy(category = assignment.category)
                        }
                        selectableAdapter.setData(allExercises, defaultExercises)
                    }
                }
            }

            dialogBinding.btnSave.setOnClickListener {
                val finalName = if (isSystemWorkout) workoutName else dialogBinding.etWorkoutName.text.toString()
                if (finalName.isBlank()) return@setOnClickListener

                val finalSelectedExercises = selectableAdapter.getSelectedExercises()
                val newAssignments = finalSelectedExercises.mapIndexed { index, ex ->
                    com.example.nutriority.data.model.WorkoutExercise(
                        workoutId = workoutWithExercises.workout.id,
                        exerciseId = ex.id,
                        category = ex.category.ifBlank { "Exercise" },
                        sets = 3,
                        reps = "10",
                        rest = "60s",
                        order = index
                    )
                }
                viewModel.updateWorkout(workoutWithExercises.workout.copy(name = finalName), newAssignments)
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
            onItemClick = { item, _, _ ->
                val itemsOnly = exerciseAdapter.currentList.filterIsInstance<WorkoutItem.ExerciseItem>()
                val index = itemsOnly.indexOfFirst { it.detail.assignment.exerciseId == item.assignment.exerciseId }
                if (index != -1) {
                    navigationViewModel.navigateToExerciseDetail(
                        item.assignment.workoutId,
                        item.assignment.exerciseId,
                        index + 1,
                        itemsOnly.size
                    )
                }
            },
            onListUpdated = { updatedList ->
            },
            onDragStart = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )

        binding.exercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseAdapter
            itemAnimator = null
            isNestedScrollingEnabled = false
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
                        if (binding.tvToolbarTitle.text != workout.workout.name) {
                            binding.tvToolbarTitle.text = workout.workout.name
                            binding.workoutTitle.text = workout.workout.name
                        }

                        isSettingInitialState = true
                        binding.switchIncludeWarmupCooldown.isChecked = workout.workout.includeWarmupCooldown
                        isSettingInitialState = false

                        updateDisplayList(workout, workout.workout.includeWarmupCooldown)
                    }
                }
            }
        }
    }

    private fun updateDisplayList(workout: WorkoutWithExercises, includeAll: Boolean) {
        val displayList = mutableListOf<WorkoutItem>()
        val assignments = workout.exerciseAssignments.sortedBy { it.assignment.order }

        val warmup = assignments.filter { it.assignment.category.equals("Warm-up", ignoreCase = true) }
        val cooldown = assignments.filter { it.assignment.category.equals("Cool-down", ignoreCase = true) }
        val main = assignments.filter { it.assignment.category.equals("Exercise", ignoreCase = true) }

        binding.workoutExerciseCount.text = main.size.toString()

        if (includeAll) {
            if (warmup.isNotEmpty()) {
                displayList.add(WorkoutItem.DividerItem("Warm-up"))
                displayList.addAll(warmup.map { WorkoutItem.ExerciseItem(it) })
            }
            displayList.add(WorkoutItem.DividerItem("Main Workout"))
            displayList.addAll(main.map { WorkoutItem.ExerciseItem(it) })
            if (cooldown.isNotEmpty()) {
                displayList.add(WorkoutItem.DividerItem("Cool-down"))
                displayList.addAll(cooldown.map { WorkoutItem.ExerciseItem(it) })
            }
        } else {
            displayList.addAll(main.map { WorkoutItem.ExerciseItem(it) })
        }

        val totalSeconds = assignments.filter {
            includeAll || it.assignment.category.equals("Exercise", ignoreCase = true)
        }.sumOf { item ->
            val durationStr = item.assignment.duration.lowercase()
            if (durationStr.contains("s")) {
                durationStr.filter { it.isDigit() }.toIntOrNull() ?: 30
            } else {
                (item.assignment.sets * 10 * 3) + (item.assignment.sets * 45)
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
