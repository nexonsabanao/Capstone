package com.example.nutriority.ui.workout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentExerciseLibraryBinding
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseLibraryFragment : Fragment() {

    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExerciseLibraryViewModel by viewModels()
    private lateinit var exerciseAdapter: ExerciseAdapter

    // Define the fixed list of filter categories for the UI.
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
        updateChipGroup(filterCategories) // Use the fixed list to create chips.
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
            onItemClick = {
                val intent = Intent(requireActivity(), ExerciseDetailActivity::class.java)
                intent.putExtra("exercise_id", it.id)
                startActivity(intent)
            },
            onListUpdated = {},
            // Provide the missing onDragStart parameter.
            onDragStart = { /* This fragment does not use drag-and-drop. */ }
        )

        binding.exercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exerciseAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allExercises.collect { exercises ->
                exerciseAdapter.submitList(exercises)
            }
        }
    }

    private fun updateChipGroup(categories: List<String>) {
        binding.targetMuscleChipGroup.removeAllViews()

        val themedContext = ContextThemeWrapper(requireContext(), R.style.FilterChip)

        // Add the "All" chip.
        val allChip = Chip(themedContext).apply {
            text = "All"
            isCheckable = true
            isChecked = viewModel.selectedTargetMuscle.value == "All"
        }
        binding.targetMuscleChipGroup.addView(allChip)

        // Add chips for each predefined category.
        for (category in categories) {
            val chip = Chip(themedContext).apply {
                text = category
                isCheckable = true
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