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
import com.example.nutriority.databinding.FragmentExerciseLibraryBinding
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.example.nutriority.ui.adapter.WorkoutItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseLibraryFragment : Fragment() {

    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WorkoutDetailViewModel by viewModels()
    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false)
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
        // Use ExerciseAdapter with drag handle hidden and target muscle displayed
        exerciseAdapter = ExerciseAdapter(
            onItemClick = { exercise, _, _ -> 
                // Show the "About Exercise" bottom sheet when an item is clicked
                AboutExerciseBottomSheet.newInstance(exercise)
                    .show(childFragmentManager, "AboutExerciseBottomSheet")
            },
            onListUpdated = { },
            onDragStart = { },
            showDragHandle = false,
            displayTargetMuscle = true
        )
        
        binding.exerciseLibraryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getAllExercises().collect { exercises ->
                // Wrap exercises in WorkoutItem.ExerciseItem for the adapter
                val workoutItems = exercises.map { WorkoutItem.ExerciseItem(it) }
                exerciseAdapter.submitList(workoutItems)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.exerciseLibraryRecyclerView.adapter = null
        _binding = null
    }
}
