package com.example.nutriority.ui.workout

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.databinding.FragmentWorkoutBinding
import com.example.nutriority.ui.adapter.WorkoutAdapter
import com.example.nutriority.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    // Use the same HomeViewModel to access the workout data
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

        binding.starterPlanCard.setOnClickListener {
            val intent = Intent(requireActivity(), PersonalizedWorkoutActivity::class.java)
            startActivity(intent)
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        workoutAdapter = WorkoutAdapter { workout ->
            val intent = Intent(requireActivity(), WorkoutDetailActivity::class.java)
            intent.putExtra("workout_id", workout.id)
            startActivity(intent)
        }
        binding.bodyFocusRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
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
        binding.bodyFocusRecyclerView.adapter = null
        _binding = null
    }
}
