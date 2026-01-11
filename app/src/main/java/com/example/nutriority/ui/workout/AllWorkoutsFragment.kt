package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.databinding.FragmentAllWorkoutsBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.WorkoutAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllWorkoutsFragment : Fragment() {

    private var _binding: FragmentAllWorkoutsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AllWorkoutsViewModel by viewModels()
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
        _binding = FragmentAllWorkoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            navigationViewModel.goBack()
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.allWorkoutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            if (adapter != workoutAdapter) {
                adapter = workoutAdapter
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.allWorkouts.collect { workouts ->
                    workoutAdapter.submitList(workouts)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
