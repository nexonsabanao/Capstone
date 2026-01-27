package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.databinding.LayoutTrainerWorkoutsBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrainerWorkoutsFragment : Fragment() {

    private var _binding: LayoutTrainerWorkoutsBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private val workoutAdapter by lazy {
        WorkoutAdapter { workout -> navigationViewModel.navigateToWorkoutDetail(workout.id) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutTrainerWorkoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.bodyFocusRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workoutAdapter
        }

        binding.starterPlanCard.setOnClickListener { navigationViewModel.setTab(4) }
        binding.exercisesLibraryCard.setOnClickListener { navigationViewModel.setTab(5) }
        binding.showAllButton.setOnClickListener { navigationViewModel.setTab(6) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // RESTORED: Observe SMART list (allWorkouts) to respect user level (Beginner/Intermediate/etc.)
                homeViewModel.allWorkouts.collect { workouts ->
                    // Since allWorkouts is already filtered by level in ViewModel, 
                    // we just need to ensure we only show official trainer IDs (<= 25)
                    workoutAdapter.submitList(workouts.filter { it.id <= 25 })
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
