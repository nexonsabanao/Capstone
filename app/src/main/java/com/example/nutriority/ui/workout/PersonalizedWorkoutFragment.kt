package com.example.nutriority.ui.workout

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentPersonalizedWorkoutBinding
import com.example.nutriority.planner.PersonalizedPlan
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalizedWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.personalizedPlanJson?.let { jsonString ->
                Log.d("WorkoutDebug", "Attempting to parse JSON: $jsonString")
                try {
                    val plan = Gson().fromJson(jsonString, PersonalizedPlan::class.java)
                    if (plan?.workoutPlan?.sessions != null) {
                        Log.d("WorkoutDebug", "Parse successful. Found ${plan.workoutPlan.sessions.size} sessions.")
                        setupRecyclerView(plan, user.lastCompletedWorkoutDay)
                    } else {
                        Log.e("WorkoutDebug", "Parsing failed or workout sessions are null.")
                    }
                } catch (e: JsonSyntaxException) {
                    Log.e("WorkoutDebug", "JSON Syntax Error. Check if the JSON is well-formed.", e)
                }
            } ?: run {
                Log.w("WorkoutDebug", "personalizedPlanJson is null for the current user.")
            }
        }
    }

    private fun setupRecyclerView(plan: PersonalizedPlan, lastCompletedDay: Int) {
        val adapter = PersonalizedWorkoutAdapter(
            requireContext(),
            plan.workoutPlan.sessions,
            lastCompletedDay,
            onStartWorkoutClicked = { dayIndex ->
                handleWorkoutStarted(dayIndex)
            },
            onRestartWorkoutClicked = {
                handleRestartWorkout()
            },
            onWorkoutClicked = { workoutId ->
                val intent = Intent(requireActivity(), WorkoutDetailActivity::class.java)
                intent.putExtra("workout_id", workoutId)
                startActivity(intent)
            }
        )
        binding.rvWorkoutPlan.layoutManager = LinearLayoutManager(context)
        binding.rvWorkoutPlan.adapter = adapter
    }

    private fun handleWorkoutStarted(dayIndex: Int) {
        // The user has started or completed the workout for the given day.
        // We now need to save this progress.
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
