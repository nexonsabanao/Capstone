package com.example.nutriority.ui.workout

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.databinding.FragmentAllWorkoutsBinding
import com.example.nutriority.ui.adapter.WorkoutAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllWorkoutsFragment : Fragment() {

    private var _binding: FragmentAllWorkoutsBinding? = null
    private val binding get() = _binding!!

    // Use the new, dedicated ViewModel to get the complete list of workouts.
    private val viewModel: AllWorkoutsViewModel by viewModels()
    private lateinit var workoutAdapter: WorkoutAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllWorkoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        workoutAdapter = WorkoutAdapter { workout ->
            // When a workout is clicked, navigate to the detail screen.
            val intent = Intent(requireActivity(), WorkoutDetailActivity::class.java)
            intent.putExtra("workout_id", workout.id)
            startActivity(intent)
        }

        binding.workoutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workoutAdapter
        }
    }

    private fun observeViewModel() {
        // Observe the list of workouts and submit it to the adapter.
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allWorkouts.collect { workouts ->
                workoutAdapter.submitList(workouts)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.workoutsRecyclerView.adapter = null
        _binding = null
    }
}
