package com.example.nutriority.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.nutriority.R
import com.example.nutriority.data.repository.MealRepository
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
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val startTime = System.currentTimeMillis()
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

                    // 2. Auth Check
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    val destination = if (firebaseUser != null) {
                        val localUser = userRepository.getInitialUser() ?: run {
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
                            R.id.action_splashFragment_to_mainTabsFragment
                        } else {
                            R.id.action_splashFragment_to_viewPagerFragment
                        }
                    } else {
                        R.id.action_splashFragment_to_viewPagerFragment
                    }

                    // 3. Ensure a minimum 2-second delay
                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime < 2000) {
                        delay(2000 - elapsedTime)
                    }

                    navigateTo(destination)

                } catch (e: Exception) {
                    Log.e("Splash", "Optimized navigation failed", e)
                    // Ensure minimum delay even on error
                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime < 2000) {
                        delay(2000 - elapsedTime)
                    }
                    navigateTo(R.id.action_splashFragment_to_viewPagerFragment)
                }
            }
        }
    }

    private fun navigateTo(destinationId: Int) {
        if (isAdded && findNavController().currentDestination?.id == R.id.splashFragment) {
            findNavController().navigate(destinationId)
        }
    }
}
