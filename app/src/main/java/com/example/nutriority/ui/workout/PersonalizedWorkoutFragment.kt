package com.example.nutriority.ui.workout

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentPersonalizedWorkoutBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.PersonalizedWorkoutAdapter
import com.example.nutriority.ui.util.BaseBindingFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonalizedWorkoutFragment : BaseBindingFragment<FragmentPersonalizedWorkoutBinding>(FragmentPersonalizedWorkoutBinding::inflate) {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val viewModel: PersonalizedWorkoutViewModel by viewModels()
    
    private lateinit var workoutAdapter: PersonalizedWorkoutAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            navigationViewModel.goBack()
        }

        binding.btnMenu.setOnClickListener {
            showPopupMenu(it)
        }

        binding.btnGenerate.setOnClickListener {
            validateAndGeneratePlan()
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun validateAndGeneratePlan() {
        val user = userViewModel.user.value
        if (user == null) {
            Toast.makeText(requireContext(), "Error: User data not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for mandatory data
        val missingFields = mutableListOf<String>()
        if (user.birthDate == null) missingFields.add("Birth Date")
        if (user.heightCm <= 0) missingFields.add("Height")
        if (user.weightKg <= 0) missingFields.add("Weight")
        if (user.gender.isBlank()) missingFields.add("Gender")
        if (user.activityLevel.isBlank()) missingFields.add("Activity Level")
        if (user.goal.isBlank()) missingFields.add("Fitness Goal")

        if (missingFields.isNotEmpty()) {
            val message = "Please complete your profile first. Missing: ${missingFields.joinToString(", ")}"
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        } else {
            userViewModel.restartWorkoutPlan()
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_personalized_workout, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_restart -> {
                    validateAndGeneratePlan()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupRecyclerView() {
        workoutAdapter = PersonalizedWorkoutAdapter(
            lastCompletedDay = 0,
            onStartWorkoutClicked = { globalDayIndex -> 
                val session = viewModel.uiState.value.sessions.find { 
                    val currentWeek = viewModel.uiState.value.currentWeek
                    val pos = viewModel.uiState.value.sessions.indexOf(it)
                    (currentWeek * 7) + pos == globalDayIndex
                }
                val id = session?.unifiedWorkoutId ?: -1
                
                if (session?.focus == "Rest Day") {
                    viewModel.completeWorkoutDay(globalDayIndex)
                } else if (id > 0) {
                    navigationViewModel.navigateToWorkoutDetail(id, session, isFromPersonalized = true, dayIndex = globalDayIndex)
                }
            },
            onWorkoutClicked = { workoutId, globalDayIndex ->
                val session = viewModel.uiState.value.sessions.find { 
                    val currentWeek = viewModel.uiState.value.currentWeek
                    val pos = viewModel.uiState.value.sessions.indexOf(it)
                    (currentWeek * 7) + pos == globalDayIndex
                }
                navigationViewModel.navigateToWorkoutDetail(workoutId, session, isFromPersonalized = true, dayIndex = globalDayIndex)
            }
        )
        
        binding.rvWorkoutPlan.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workoutAdapter
            itemAnimator = null 
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    if (state.isInitialLoading) {
                        binding.progressBar.isVisible = true
                        binding.rvWorkoutPlan.isVisible = false
                        binding.initialView.isVisible = false
                        binding.tvTitle.isVisible = false
                        binding.btnMenu.isVisible = false
                        return@collectLatest
                    }

                    binding.progressBar.isVisible = state.isLoading
                    binding.initialView.isVisible = !state.hasPlan && !state.isLoading
                    binding.rvWorkoutPlan.isVisible = state.hasPlan && !state.isLoading
                    binding.tvTitle.isVisible = state.hasPlan && !state.isLoading
                    
                    // Hide menu if there's no plan and we're not currently generating/loading
                    binding.btnMenu.isVisible = state.hasPlan

                    if (state.hasPlan) {
                        workoutAdapter.updateLastCompletedDay(state.lastCompletedDay)
                        workoutAdapter.submitList(state.sessions)
                        updateHeaderText(state.sessions, state.lastCompletedDay, state.currentWeek)
                    } else {
                        binding.tvTitle.text = "Workout Plan"
                    }
                }
            }
        }
    }

    private fun updateHeaderText(sessions: List<com.example.nutriority.planner.WorkoutSession>, lastCompletedDay: Int, currentWeek: Int) {
        val totalDays = 28
        if (lastCompletedDay < totalDays) {
            val sessionIndexInWeek = lastCompletedDay % 7
            val session = sessions.getOrNull(sessionIndexInWeek)
            val focusText = session?.focus?.let { 
                if (it == "Rest Day") "Recover & Rebuild" else it 
            } ?: "Keep Going!"
            binding.tvTitle.text = "Week ${currentWeek + 1} · Day ${lastCompletedDay + 1}: $focusText"
        } else {
            binding.tvTitle.text = "All 4 Weeks Completed!"
        }
    }
}
