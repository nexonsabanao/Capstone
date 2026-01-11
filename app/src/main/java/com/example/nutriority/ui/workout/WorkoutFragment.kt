package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentWorkoutBinding
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var workoutAdapter: WorkoutAdapter

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
            findNavController().navigate(R.id.action_navigation_workout_to_personalizedWorkoutFragment)
        }

        binding.exercisesLibraryCard.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_workout_to_exerciseLibraryFragment)
        }

        binding.showAllButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_workout_to_allWorkoutsFragment)
        }
    }

    private fun setupRecyclerView() {
        workoutAdapter = WorkoutAdapter { workout ->
            val bundle = Bundle().apply {
                putInt("workout_id", workout.id)
            }
            findNavController().navigate(R.id.action_workout_to_detail, bundle)
        }
        binding.bodyFocusRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workoutAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
