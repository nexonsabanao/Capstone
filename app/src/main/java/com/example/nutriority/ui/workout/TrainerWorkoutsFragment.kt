package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.databinding.LayoutTrainerWorkoutsBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.home.HomeViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrainerWorkoutsFragment : BaseBindingFragment<LayoutTrainerWorkoutsBinding>(LayoutTrainerWorkoutsBinding::inflate) {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private val workoutAdapter by lazy {
        WorkoutAdapter { workout -> navigationViewModel.navigateToWorkoutDetail(workout.id) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.bodyFocusRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workoutAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.starterPlanCard.setOnClickListener { navigationViewModel.setTab(4) }
        binding.exercisesLibraryCard.setOnClickListener { navigationViewModel.setTab(5) }
        binding.showAllButton.setOnClickListener { navigationViewModel.setTab(6) }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.allWorkouts.collect { workouts ->
                    workoutAdapter.submitList(workouts.filter { it.id <= 25 })
                }
            }
        }
    }
}
