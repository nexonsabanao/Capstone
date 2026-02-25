package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.data.model.Workout
import com.example.nutriority.databinding.LayoutTrainerWorkoutsBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.home.HomeViewModel
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrainerWorkoutsFragment : BaseBindingFragment<LayoutTrainerWorkoutsBinding>(LayoutTrainerWorkoutsBinding::inflate) {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()

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
                // Combine workouts from HomeViewModel and the User from UserViewModel
                combine(
                    homeViewModel.unfilteredWorkouts,
                    userViewModel.user.asFlow()
                ) { workouts, user ->
                    workouts to user
                }.collectLatest { (workouts, user) ->
                    // 1. Filter for official workouts only
                    val officialWorkouts = workouts.filter { it.category == "Official" || it.id in 1..25 }

                    if (officialWorkouts.isNotEmpty() && user != null) {
                        // 2. Determine allowed difficulties based on user activity level (lowercase for safety)
                        val activity = user.activityLevel.lowercase().trim()
                        val allowedDifficulties = when {
                            activity.contains("sedentary") -> listOf("Beginner")
                            activity.contains("lightly active") -> listOf("Beginner", "Intermediate")
                            activity.contains("active") -> listOf("Intermediate", "Advanced")
                            else -> listOf("Beginner") // Safe default
                        }
                        
                        // 3. Apply the strict filter
                        val filteredList = officialWorkouts.filter { it.difficulty in allowedDifficulties }

                        // 4. Update UI
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
    }
}
