package com.example.nutriority.ui.workout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
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
import kotlinx.coroutines.flow.collectLatest
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
            onStartWorkoutClicked = { dayIndex -> 
                val user = userViewModel.user.value
                val json = user?.personalizedPlanJson
                if (!json.isNullOrBlank()) {
                    try {
                        val plan = Gson().fromJson(json, WorkoutPlan::class.java)
                        val session = plan?.sessions?.getOrNull(dayIndex)
                        val id = session?.unifiedWorkoutId ?: -1
                        
                        if (session?.focus == "Rest Day") {
                            handleWorkoutStarted(dayIndex)
                        } else if (id > 0) {
                            navigationViewModel.navigateToWorkoutDetail(id, isFromPersonalized = true, dayIndex = dayIndex)
                        }
                    } catch (e: Exception) {
                        Log.e("Workout", "Navigation error", e)
                    }
                }
            },
            onRestartWorkoutClicked = { handleRestartWorkout() },
            onWorkoutClicked = { workoutId, dayIndex ->
                navigationViewModel.navigateToWorkoutDetail(workoutId, isFromPersonalized = true, dayIndex = dayIndex)
            }
        )
        
        binding.rvWorkoutPlan.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workoutAdapter
            itemAnimator = null 
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Use asFlow() for more reliable real-time updates during restoration
                userViewModel.user.asFlow().collectLatest { user ->
                    if (user != null && !user.personalizedPlanJson.isNullOrBlank()) {
                        try {
                            val workoutPlan = Gson().fromJson(user.personalizedPlanJson, WorkoutPlan::class.java)
                            if (workoutPlan?.sessions != null) {
                                binding.rvWorkoutPlan.visibility = View.VISIBLE
                                updateUI(workoutPlan, user.lastCompletedWorkoutDay)
                            }
                        } catch (e: JsonSyntaxException) {
                            Log.e("WorkoutDebug", "JSON Syntax Error", e)
                        }
                    } else {
                        // Empty state handling
                        binding.rvWorkoutPlan.visibility = View.GONE
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
