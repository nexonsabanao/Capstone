package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.data.model.WorkoutExercise
import com.example.nutriority.data.model.WorkoutExerciseWithDetail
import com.example.nutriority.data.model.Exercise
import com.example.nutriority.databinding.FragmentExerciseLibraryBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.example.nutriority.ui.adapter.WorkoutItem
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseLibraryFragment : BaseBindingFragment<FragmentExerciseLibraryBinding>(FragmentExerciseLibraryBinding::inflate) {

    private val viewModel: WorkoutDetailViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    
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
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.exerciseLibraryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getAllExercises().collect { exercises ->
                    val workoutItems = exercises.map { exercise -> 
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
