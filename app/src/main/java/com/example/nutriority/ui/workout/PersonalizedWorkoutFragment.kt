package com.example.nutriority.ui.workout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentPersonalizedWorkoutBinding
import com.example.nutriority.planner.WorkoutPlan
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.PersonalizedWorkoutAdapter
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonalizedWorkoutFragment : Fragment() {

    private var _binding: FragmentPersonalizedWorkoutBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    
    private lateinit var workoutAdapter: PersonalizedWorkoutAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalizedWorkoutBinding.inflate(inflater, container, false)
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
        workoutAdapter = PersonalizedWorkoutAdapter(
            emptyList(),
            0,
            onStartWorkoutClicked = { dayIndex -> handleWorkoutStarted(dayIndex) },
            onRestartWorkoutClicked = { handleRestartWorkout() },
            onWorkoutClicked = { workoutId ->
                navigationViewModel.navigateToWorkoutDetail(workoutId)
            }
        )
        
        binding.rvWorkoutPlan.apply {
            layoutManager = LinearLayoutManager(context)
            if (adapter != workoutAdapter) {
                adapter = workoutAdapter
            }
            itemAnimator = null 
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Lazy UI Fix: Only update when resumed to keep the app smooth
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                userViewModel.user.observe(viewLifecycleOwner) { user ->
                    user?.personalizedPlanJson?.let { jsonString ->
                        try {
                            val workoutPlan = Gson().fromJson(jsonString, WorkoutPlan::class.java)
                            if (workoutPlan?.sessions != null) {
                                updateUI(workoutPlan, user.lastCompletedWorkoutDay)
                            }
                        } catch (e: JsonSyntaxException) {
                            Log.e("WorkoutDebug", "JSON Syntax Error in plan", e)
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(plan: WorkoutPlan, lastCompletedDay: Int) {
        val currentDay = lastCompletedDay + 1
        if (currentDay <= plan.sessions.size) {
            val session = plan.sessions[lastCompletedDay]
            val focusText = if (session.focus == "Rest Day") "Recover & Rebuild" else session.focus
            binding.tvTitle.text = "Day $currentDay: $focusText"
        } else {
            binding.tvTitle.text = "Plan Completed!"
        }

        workoutAdapter.updateData(plan.sessions, lastCompletedDay)
    }

    private fun handleWorkoutStarted(dayIndex: Int) {
        lifecycleScope.launch {
            userViewModel.completeWorkoutDay(dayIndex)
        }
    }

    private fun handleRestartWorkout() {
        lifecycleScope.launch {
            userViewModel.restartWorkoutPlan()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
