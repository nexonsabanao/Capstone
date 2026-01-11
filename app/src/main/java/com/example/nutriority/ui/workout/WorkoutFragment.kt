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
import com.example.nutriority.databinding.FragmentWorkoutBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    
    private val workoutAdapter by lazy {
        WorkoutAdapter { workout ->
            navigationViewModel.navigateToWorkoutDetail(workout.id)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.starterPlanCard.setOnClickListener {
            navigationViewModel.setTab(4)
        }

        binding.exercisesLibraryCard.setOnClickListener {
            navigationViewModel.setTab(5)
        }

        binding.showAllButton.setOnClickListener {
            navigationViewModel.setTab(6)
        }
    }

    private fun setupRecyclerView() {
        // UI Fix: Immediately submit list if data is already in ViewModel (Pre-loaded)
        val workouts = homeViewModel.allWorkouts.value
        if (workouts.isNotEmpty()) {
            workoutAdapter.submitList(workouts)
        }

        binding.bodyFocusRecyclerView.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            if (adapter != workoutAdapter) {
                adapter = workoutAdapter
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // OPTIMIZATION: Only collect data when the fragment is actually RESUMED (on screen)
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                homeViewModel.allWorkouts.collect { workouts ->
                    if (workouts.isNotEmpty()) {
                        workoutAdapter.submitList(workouts)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
