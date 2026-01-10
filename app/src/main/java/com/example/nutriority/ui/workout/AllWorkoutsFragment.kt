package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentAllWorkoutsBinding
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllWorkoutsFragment : Fragment() {

    private var _binding: FragmentAllWorkoutsBinding? = null
    private val binding get() = _binding!!
    
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var workoutAdapter: WorkoutAdapter

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
            findNavController().navigateUp()
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        workoutAdapter = WorkoutAdapter { workout ->
            val bundle = Bundle().apply {
                putInt("workout_id", workout.id)
            }
            findNavController().navigate(R.id.action_allWorkoutsFragment_to_workoutDetailFragment, bundle)
        }
        
        binding.allWorkoutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.allWorkouts.collect { workouts ->
                workoutAdapter.submitList(workouts)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.allWorkoutsRecyclerView.adapter = null
        _binding = null
    }
}
