package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.databinding.FragmentExerciseLibraryBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.example.nutriority.ui.adapter.WorkoutItem
import com.example.nutriority.ui.util.BaseBindingFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseLibraryFragment : BaseBindingFragment<FragmentExerciseLibraryBinding>(FragmentExerciseLibraryBinding::inflate) {

    private val viewModel: WorkoutDetailViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    
    private val searchQuery = MutableStateFlow("")
    private val selectedTarget = MutableStateFlow("All")

    private val exerciseAdapter by lazy {
        ExerciseAdapter(
            onItemClick = { item, _, _ -> 
                AboutExerciseBottomSheet.newInstance(item.exercise)
                    .show(childFragmentManager, "AboutExerciseBottomSheet")
            },
            onListUpdated = { },
            onDragStart = { },
            isLibraryView = true 
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { navigationViewModel.goBack() }
        setupRecyclerView()
        setupFilters()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.exerciseLibraryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFilters() {
        val targets = listOf("All", "Abs", "Arms", "Back", "Chest", "Legs", "Shoulders", "Full Body")
        binding.muscleChipGroup.removeAllViews()
        
        targets.forEach { target ->
            val chip = LayoutInflater.from(requireContext()).inflate(R.layout.layout_filter_chip, binding.muscleChipGroup, false) as Chip
            chip.text = target
            chip.id = View.generateViewId()
            chip.isChecked = target == "All"
            binding.muscleChipGroup.addView(chip)
        }

        binding.muscleChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = group.findViewById<Chip>(checkedIds.firstOrNull() ?: -1)
            selectedTarget.value = chip?.text?.toString() ?: "All"
        }

        binding.etSearch.doAfterTextChanged { 
            searchQuery.value = it?.toString() ?: ""
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.getAllExercises(),
                    searchQuery,
                    selectedTarget
                ) { exercises, query, target ->
                    exercises.filter { exercise ->
                        val matchesSearch = query.isEmpty() || exercise.name.contains(query, ignoreCase = true) || 
                                           exercise.target.contains(query, ignoreCase = true)
                        val matchesTarget = target == "All" || exercise.target.contains(target, ignoreCase = true)
                        matchesSearch && matchesTarget
                    }
                }.collect { filteredExercises ->
                    val workoutItems = filteredExercises.map { exercise -> 
                        WorkoutItem.ExerciseItem(
                            WorkoutExerciseWithDetail(
                                assignment = WorkoutExercise(
                                    workoutId = 0,
                                    exerciseId = exercise.id,
                                    category = "Library",
                                    sets = 0,
                                    reps = "",
                                    rest = ""
                                ),
                                exercise = exercise
                            )
                        )
                    }
                    exerciseAdapter.submitList(workoutItems)
                }
            }
        }
    }
}
