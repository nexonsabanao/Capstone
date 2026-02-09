package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.databinding.FragmentAllWorkoutsBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllWorkoutsFragment : BaseBindingFragment<FragmentAllWorkoutsBinding>(FragmentAllWorkoutsBinding::inflate) {

    private val viewModel: AllWorkoutsViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    
    private val workoutAdapter by lazy {
        WorkoutAdapter { workout ->
            navigationViewModel.navigateToWorkoutDetail(workout.id)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { navigationViewModel.goBack() }
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.allWorkoutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWorkouts.collect { workouts ->
                    val officialWorkouts = workouts.filter { it.id <= 25 }
                    workoutAdapter.submitList(officialWorkouts)
                }
            }
        }
    }
}
