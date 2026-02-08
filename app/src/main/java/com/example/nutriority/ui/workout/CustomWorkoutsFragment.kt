package com.example.nutriority.ui.workout

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CustomWorkoutsFragment : Fragment() {

    private var _binding: LayoutCustomWorkoutsBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val viewModel: WorkoutDetailViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private val customAdapter by lazy {
        CustomWorkoutAdapter(
            onClick = { workout -> 
                navigationViewModel.navigateToWorkoutDetail(workout.id) 
            },
            onDeleteClick = { workout ->
                showDeleteConfirmation(workout)
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutCustomWorkoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.rvCustomWorkouts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = customAdapter
        }

        binding.btnCreateWorkout.setOnClickListener {
            showCreateWorkoutDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.unfilteredWorkouts.collect { workouts ->
                    // FIX: Personalized workouts use negative IDs. User-created use positive IDs > 25.
                    val customOnes = workouts.filter { it.id > 25 }
                    customAdapter.submitList(customOnes)
                    
                    binding.emptyStateLayout.visibility = if (customOnes.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvCustomWorkouts.visibility = if (customOnes.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showDeleteConfirmation(workout: Workout) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Workout")
            .setMessage("Are you sure you want to delete '${workout.name}'? This action cannot be undone.")
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
            
            // Populate Target Muscle Chips
            val uniqueTargets = allExercises.flatMap { it.target.split(",") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            dialogBinding.targetMuscleChipGroup.removeAllViews()
            
            // Add "All" chip
            val allChip = LayoutInflater.from(requireContext()).inflate(R.layout.layout_filter_chip, dialogBinding.targetMuscleChipGroup, false) as Chip
            allChip.text = "All"
            allChip.isChecked = true
            allChip.id = View.generateViewId()
            dialogBinding.targetMuscleChipGroup.addView(allChip)

            uniqueTargets.forEach { target ->
                val chip = LayoutInflater.from(requireContext()).inflate(R.layout.layout_filter_chip, dialogBinding.targetMuscleChipGroup, false) as Chip
                chip.text = target
                chip.id = View.generateViewId()
                dialogBinding.targetMuscleChipGroup.addView(chip)
            }

            selectableAdapter.setData(allExercises, emptyList())

            fun applyFilters() {
                val category = when (dialogBinding.categoryChipGroup.checkedChipId) {
                    R.id.chip_warmup -> "warmup"
                    R.id.chip_cooldown -> "cooldown"
                    else -> "Exercise"
                }
                
                val checkedTargetId = dialogBinding.targetMuscleChipGroup.checkedChipId
                val selectedTarget = if (checkedTargetId != View.NO_ID) {
                    dialogBinding.targetMuscleChipGroup.findViewById<Chip>(checkedTargetId)?.text?.toString() ?: "All"
                } else "All"

                selectableAdapter.setFilter(category, if (selectedTarget == "All") null else selectedTarget)
            }

            dialogBinding.categoryChipGroup.setOnCheckedStateChangeListener { _, _ -> applyFilters() }
            dialogBinding.targetMuscleChipGroup.setOnCheckedStateChangeListener { _, _ -> applyFilters() }

            dialogBinding.btnSave.setOnClickListener {
                val workoutName = dialogBinding.etWorkoutName.text.toString().trim()
                if (workoutName.isBlank()) {
                    Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val selected = selectableAdapter.getSelectedExercises()
                if (selected.isEmpty()) {
                    Toast.makeText(context, "Select at least one exercise", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newId = (System.currentTimeMillis() % 1000000).toInt() + 100 
                
                val newWorkout = Workout(
                    id = newId,
                    name = workoutName,
                    category = "Custom",
                    difficulty = "Intermediate",
                    imageName = "" 
                )

                val assignments = selected.mapIndexed { index, ex ->
                    WorkoutExercise(
                        workoutId = newId, 
                        exerciseId = ex.id, 
                        category = ex.category, 
                        sets = 3, 
                        reps = "10", 
                        rest = "60s", 
                        duration = "", 
                        order = index
                    )
                }

                viewModel.updateWorkout(newWorkout, assignments)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Custom workout created!", Toast.LENGTH_SHORT).show()
            }

            dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }
            dialogBinding.loadingProgress.visibility = View.GONE
            dialogBinding.contentLayout.visibility = View.VISIBLE
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
