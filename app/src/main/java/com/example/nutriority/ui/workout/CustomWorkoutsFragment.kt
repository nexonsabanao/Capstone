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

        dialogBinding.headerContainer.findViewById<TextView>(R.id.workout_title)?.text = "Create Workout"
        dialogBinding.btnSave.text = "CREATE WORKOUT"
        dialogBinding.workoutNameLayout.visibility = View.VISIBLE

        val selectableAdapter = SelectableExerciseAdapter { _, _ -> }
        dialogBinding.rvSelectExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectableAdapter
        }

        dialogBinding.categoryChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when (checkedIds.firstOrNull()) {
                R.id.chip_warmup -> "warmup"
                R.id.chip_cooldown -> "cooldown"
                else -> "Exercise"
            }
            selectableAdapter.setFilter(category)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val allExercises = viewModel.getAllExercises().first()
            selectableAdapter.setData(allExercises, emptyList())

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
                    WorkoutExercise(newId, ex.id, ex.category, 3, "10", "60s", "", index)
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
