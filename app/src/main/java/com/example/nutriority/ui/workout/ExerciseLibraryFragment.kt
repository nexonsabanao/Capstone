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
import com.example.nutriority.databinding.FragmentExerciseLibraryBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.ExerciseAdapter
import com.example.nutriority.ui.adapter.WorkoutItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseLibraryFragment : Fragment() {

    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WorkoutDetailViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    
    private val exerciseAdapter by lazy {
        ExerciseAdapter(
            onItemClick = { exercise, _, _ -> 
                AboutExerciseBottomSheet.newInstance(exercise)
                    .show(childFragmentManager, "AboutExerciseBottomSheet")
            },
            onListUpdated = { },
            onDragStart = { },
            showDragHandle = false,
            displayTargetMuscle = true
        )
    }

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
            navigationViewModel.goBack()
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.exerciseLibraryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            if (adapter != exerciseAdapter) {
                adapter = exerciseAdapter
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Lazy UI Fix: Only update when resumed to keep the app smooth
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.getAllExercises().collect { exercises ->
                    val workoutItems = exercises.map { WorkoutItem.ExerciseItem(it) }
                    exerciseAdapter.submitList(workoutItems)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
