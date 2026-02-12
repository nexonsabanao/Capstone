package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.data.model.Workout
import com.example.nutriority.databinding.LayoutTrainerWorkoutsBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.home.HomeViewModel
import com.example.nutriority.ui.profile.ProfileViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrainerWorkoutsFragment : BaseBindingFragment<LayoutTrainerWorkoutsBinding>(LayoutTrainerWorkoutsBinding::inflate) {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()

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
                // Combine unfiltered workouts from HomeViewModel and the UI state from ProfileViewModel
                // This approach is more stable and avoids complex type inference issues
                combine(
                    homeViewModel.unfilteredWorkouts,
                    profileViewModel.uiState
                ) { workouts, profileState ->
                    workouts to profileState.user
                }.collectLatest { (workouts, user) ->
                    // Filter for official workouts
                    val officialWorkouts = workouts.filter { it.category == "Official" || it.id in 1..25 }

                    if (officialWorkouts.isNotEmpty()) {
                        val filteredList = if (user != null) {
                            val allowedDifficulties = when (user.activityLevel) {
                                "Sedentary" -> listOf("Beginner")
                                "Lightly active" -> listOf("Beginner", "Intermediate")
                                "Active" -> listOf("Intermediate", "Advanced")
                                else -> listOf("Beginner", "Intermediate", "Advanced")
                            }
                            
                            val filtered = officialWorkouts.filter { it.difficulty in allowedDifficulties }
                            if (filtered.isEmpty()) officialWorkouts.take(10) else filtered
                        } else {
                            officialWorkouts.take(10)
                        }

                        workoutAdapter.submitList(filteredList)
                        binding.loadingProgress.visibility = View.GONE
                        binding.contentScrollView.visibility = View.VISIBLE
                    } else if (homeViewModel.isDataReady.value) {
                        binding.loadingProgress.visibility = View.GONE
                        binding.contentScrollView.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Secondary observer for isDataReady to ensure loading progress is hidden correctly
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.isDataReady.collect { isReady ->
                    if (isReady) {
                        binding.loadingProgress.visibility = View.GONE
                        binding.contentScrollView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}
