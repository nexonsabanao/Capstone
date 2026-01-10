package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.databinding.FragmentExerciseLibraryBinding
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.example.nutriority.ui.adapter.WorkoutItem
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseLibraryFragment : Fragment() {

    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExerciseLibraryViewModel by viewModels()
    private lateinit var exerciseAdapter: ExerciseAdapter

    private val filterCategories = listOf("Warm-up", "Cool-down", "Shoulders", "Abs", "Legs", "Back", "Chest", "Arms", "Neck")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupChipGroupListener()
        updateChipGroup(filterCategories)
    }

    private fun setupChipGroupListener() {
        binding.targetMuscleChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val targetMuscle = if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                chip.text.toString()
            } else {
                "All"
            }
            viewModel.setTargetMuscle(targetMuscle)
        }
    }

    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseAdapter(
            onItemClick = { exercise, _, _ ->
                // Use the new separate BottomSheet class to show about exercise dialog
                AboutExerciseBottomSheet.newInstance(exercise)
                    .show(parentFragmentManager, "AboutExerciseBottomSheet")
            },
            onListUpdated = {},
            onDragStart = { /* Not used in library */ },
            showDragHandle = false,
            displayTargetMuscle = true
        )

        binding.exercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exerciseAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allExercises.collect { exercises ->
                // Map the list of Exercises to a list of WorkoutItems.ExerciseItem
                val workoutItems = exercises.map { WorkoutItem.ExerciseItem(it) }
                exerciseAdapter.submitList(workoutItems)
            }
        }
    }

    private fun updateChipGroup(categories: List<String>) {
        binding.targetMuscleChipGroup.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        // 1. Add the "All" chip
        val allChip = inflater.inflate(com.example.nutriority.R.layout.item_filter_chip, binding.targetMuscleChipGroup, false) as Chip
        allChip.apply {
            text = "All"
            isChecked = viewModel.selectedTargetMuscle.value == "All"
        }
        binding.targetMuscleChipGroup.addView(allChip)

        // 2. Add chips for each predefined category
        for (category in categories) {
            val chip = inflater.inflate(com.example.nutriority.R.layout.item_filter_chip, binding.targetMuscleChipGroup, false) as Chip
            chip.apply {
                text = category
                isChecked = viewModel.selectedTargetMuscle.value == category
            }
            binding.targetMuscleChipGroup.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.exercisesRecyclerView.adapter = null
        _binding = null
    }
}
