package com.example.nutriority.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.RecommendedWorkoutRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.databinding.FragmentSplashBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.planner.WorkoutPlan
import com.example.nutriority.planner.WorkoutPlanner
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : BaseBindingFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var mealRepository: MealRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var workoutPlanner: WorkoutPlanner
    @Inject lateinit var gson: Gson
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. FAST CHECK: If data exists, skip mandatory wait
                val hasExercises = withContext(Dispatchers.IO) {
                    workoutRepository.getAllExercises().first().isNotEmpty()
                }

                if (!hasExercises) {
                    // Only perform block-level sync if library is totally empty
                    coroutineScope {
                        awaitAll(
                            async { workoutRepository.syncExercisesFromCloud() },
                            async { mealRepository.syncMealsFromCloud() }
                        )
                    }
                }
                
                // 2. Auth Check & Instant Navigation
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    val localUser = userRepository.getInitialUser() ?: run {
                        // Minimal restore if local is missing
                        userRepository.restoreUserFromCloud()
                        userRepository.getInitialUser()
                    }

                    if (localUser != null) {
                        // Background Hydration (Non-blocking)
                        if (!localUser.personalizedPlanJson.isNullOrBlank()) {
                            launch(Dispatchers.IO) {
                                try {
                                    val plan = gson.fromJson(localUser.personalizedPlanJson, WorkoutPlan::class.java)
                                    workoutPlanner.syncPlanToDatabase(plan)
                                } catch (e: Exception) { }
                            }
                        }
                        findNavController().navigate(R.id.action_splashFragment_to_mainTabsFragment)
                    } else {
                        findNavController().navigate(R.id.action_splashFragment_to_viewPagerFragment)
                    }
                } else {
                    // New user: brief delay for branding
                    delay(1000)
                    findNavController().navigate(R.id.action_splashFragment_to_viewPagerFragment)
                }
            } catch (e: Exception) {
                Log.e("Splash", "Optimized navigation failed", e)
                findNavController().navigate(R.id.action_splashFragment_to_viewPagerFragment)
            }
        }
    }
}
