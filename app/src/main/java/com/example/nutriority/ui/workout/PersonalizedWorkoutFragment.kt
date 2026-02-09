package com.example.nutriority.ui.workout

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentPersonalizedWorkoutBinding
import com.example.nutriority.planner.WorkoutPlan
import com.example.nutriority.planner.WorkoutPlanner
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.adapter.PersonalizedWorkoutAdapter
import com.example.nutriority.ui.util.BaseBindingFragment
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PersonalizedWorkoutFragment : BaseBindingFragment<FragmentPersonalizedWorkoutBinding>(FragmentPersonalizedWorkoutBinding::inflate) {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    
    @Inject lateinit var workoutPlanner: WorkoutPlanner
    @Inject lateinit var gson: Gson
    
    private lateinit var workoutAdapter: PersonalizedWorkoutAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            navigationViewModel.goBack()
        }

        binding.btnMenu.setOnClickListener {
            showPopupMenu(it)
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_personalized_workout, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_restart -> {
                    handleRestartWorkout()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupRecyclerView() {
        workoutAdapter = PersonalizedWorkoutAdapter(
            0,
            onStartWorkoutClicked = { globalDayIndex -> 
                val user = userViewModel.user.value
                val json = user?.personalizedPlanJson
                if (!json.isNullOrBlank()) {
                    try {
                        val plan = gson.fromJson(json, WorkoutPlan::class.java)
                        val session = plan?.sessions?.getOrNull(globalDayIndex)
                        val id = session?.unifiedWorkoutId ?: -1
                        
                        if (session?.focus == "Rest Day") {
                            handleWorkoutStarted(globalDayIndex)
                        } else if (id > 0) {
                            navigationViewModel.navigateToWorkoutDetail(id, isFromPersonalized = true, dayIndex = globalDayIndex)
                        }
                    } catch (e: Exception) {
                        Log.e("Workout", "Navigation error", e)
                    }
                }
            },
            onWorkoutClicked = { workoutId, globalDayIndex ->
                navigationViewModel.navigateToWorkoutDetail(workoutId, isFromPersonalized = true, dayIndex = globalDayIndex)
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
                // Observe loading state
                launch {
                    userViewModel.isLoading.collect { isLoading ->
                        showLoading(isLoading)
                    }
                }

                // Observe user data changes
                launch {
                    userViewModel.user.asFlow()
                        .map { it?.personalizedPlanJson to it?.lastCompletedWorkoutDay }
                        .distinctUntilChanged()
                        .collectLatest { (json, lastCompletedDay) ->
                            if (!json.isNullOrBlank()) {
                                try {
                                    val fullPlan = gson.fromJson(json, WorkoutPlan::class.java)
                                    if (fullPlan?.sessions != null) {
                                        val safeLastCompleted = lastCompletedDay ?: 0
                                        
                                        // We no longer call ensurePlanSynced here. 
                                        // The plan is synced once when generated or restored in UserViewModel.
                                        
                                        val currentWeek = (safeLastCompleted / 7).coerceAtMost(3)
                                        val startIndex = currentWeek * 7
                                        val endIndex = (startIndex + 7).coerceAtMost(fullPlan.sessions.size)
                                        val activeSessions = fullPlan.sessions.subList(startIndex, endIndex)
                                        
                                        workoutAdapter.updateLastCompletedDay(safeLastCompleted)
                                        workoutAdapter.submitList(activeSessions)
                                        updateHeaderText(activeSessions, safeLastCompleted, currentWeek)
                                        
                                        binding.rvWorkoutPlan.visibility = View.VISIBLE
                                    }
                                } catch (e: JsonSyntaxException) {
                                    Log.e("WorkoutDebug", "JSON Syntax Error", e)
                                }
                            } else {
                                binding.rvWorkoutPlan.visibility = View.GONE
                            }
                        }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.rvWorkoutPlan.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.btnMenu.isEnabled = !isLoading
        binding.tvTitle.visibility = if (isLoading) View.GONE else View.VISIBLE
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
}
