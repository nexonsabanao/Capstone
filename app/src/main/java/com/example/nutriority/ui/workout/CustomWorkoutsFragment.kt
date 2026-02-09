package com.example.nutriority.ui.workout

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.data.model.Workout
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.databinding.DialogEditWorkoutBinding
import com.example.nutriority.databinding.LayoutCustomWorkoutsBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.CustomWorkoutAdapter
import com.example.nutriority.ui.adapter.SelectableExerciseAdapter
import com.example.nutriority.ui.home.HomeViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.KeyboardUtil
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CustomWorkoutsFragment : BaseBindingFragment<LayoutCustomWorkoutsBinding>(LayoutCustomWorkoutsBinding::inflate) {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val viewModel: WorkoutDetailViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private val customAdapter by lazy {
        CustomWorkoutAdapter(
            onClick = { workout -> navigationViewModel.navigateToWorkoutDetail(workout.id) },
            onDeleteClick = { workout -> showDeleteConfirmation(workout) }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvCustomWorkouts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = customAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateWorkout.setOnClickListener { showCreateWorkoutDialog() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.unfilteredWorkouts.collect { workouts ->
                val customOnes = workouts.filter { it.id in 26..999 }
                customAdapter.submitList(customOnes)
                binding.emptyStateLayout.visibility = if (customOnes.isEmpty()) View.VISIBLE else View.GONE
                binding.rvCustomWorkouts.visibility = if (customOnes.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun showDeleteConfirmation(workout: Workout) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Workout")
            .setMessage("Are you sure you want to delete '${workout.name}'?")
            .setPositiveButton("DELETE") { _, _ ->
                viewModel.deleteWorkout(workout)
                Toast.makeText(requireContext(), "Workout deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showCreateWorkoutDialog() {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
        val dialogBinding = DialogEditWorkoutBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        dialogBinding.workoutTitle.text = "Create Workout"
        dialogBinding.btnSave.text = "CREATE WORKOUT"
        dialogBinding.workoutNameLayout.visibility = View.VISIBLE

        val selectableAdapter = SelectableExerciseAdapter { _, _ -> }
        dialogBinding.rvSelectExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectableAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val allExercises = viewModel.getAllExercises().first()
            setupTargetChips(dialogBinding, allExercises, selectableAdapter)
            selectableAdapter.setData(allExercises, emptyList())

            dialogBinding.btnSave.setOnClickListener {
                // BUG FIX: Hide keyboard when user clicks save
                KeyboardUtil.hideKeyboard(dialog.window?.decorView ?: dialogBinding.root)
                
                val workoutName = dialogBinding.etWorkoutName.text.toString().trim()
                if (workoutName.isBlank()) {
                    Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val selected = selectableAdapter.getSelectedExercises()
                if (selected.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select at least one exercise", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newId = (System.currentTimeMillis() % 900).toInt() + 100 
                val newWorkout = Workout(id = newId, name = workoutName, category = "Custom", difficulty = "Intermediate")
                val assignments = selected.mapIndexed { i, ex ->
                    WorkoutExercise(workoutId = newId, exerciseId = ex.id, category = ex.category, sets = 3, reps = "10", rest = "60s", order = i)
                }
                viewModel.updateWorkout(newWorkout, assignments)
                dialog.dismiss()
            }
            dialogBinding.btnBack.setOnClickListener { 
                KeyboardUtil.hideKeyboard(dialog.window?.decorView ?: dialogBinding.root)
                dialog.dismiss() 
            }
            dialogBinding.loadingProgress.visibility = View.GONE
            dialogBinding.contentLayout.visibility = View.VISIBLE
        }
        dialog.show()
    }

    private fun setupTargetChips(dialogBinding: DialogEditWorkoutBinding, all: List<com.example.nutriority.data.model.Exercise>, adapter: SelectableExerciseAdapter) {
        val targets = listOf("Abs", "Arms", "Back", "Chest", "Legs", "Shoulders", "Full Body")
        dialogBinding.targetMuscleChipGroup.removeAllViews()
        val allChip = LayoutInflater.from(requireContext()).inflate(R.layout.layout_filter_chip, dialogBinding.targetMuscleChipGroup, false) as Chip
        allChip.text = "All"; allChip.isChecked = true; allChip.id = View.generateViewId()
        dialogBinding.targetMuscleChipGroup.addView(allChip)

        targets.forEach { t ->
            val chip = LayoutInflater.from(requireContext()).inflate(R.layout.layout_filter_chip, dialogBinding.targetMuscleChipGroup, false) as Chip
            chip.text = t; chip.id = View.generateViewId()
            dialogBinding.targetMuscleChipGroup.addView(chip)
        }

        fun filter() {
            val cat = when(dialogBinding.categoryChipGroup.checkedChipId) {
                R.id.chip_warmup -> "warmup"; R.id.chip_cooldown -> "cooldown"; else -> "Exercise"
            }
            val chip = dialogBinding.targetMuscleChipGroup.findViewById<Chip>(dialogBinding.targetMuscleChipGroup.checkedChipId)
            adapter.setFilter(cat, if (chip?.text == "All") null else chip?.text?.toString())
        }
        dialogBinding.categoryChipGroup.setOnCheckedStateChangeListener { _, _ -> filter() }
        dialogBinding.targetMuscleChipGroup.setOnCheckedStateChangeListener { _, _ -> filter() }
    }
}
