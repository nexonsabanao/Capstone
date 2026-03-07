package com.example.nutriority.ui

import android.content.Context
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
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
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
                    // 1. Initial Library Sync
                    val hasExercises = withContext(Dispatchers.IO) {
                        workoutRepository.getAllExercises().first().isNotEmpty()
                    }
                    if (!hasExercises) {
                        coroutineScope {
                            awaitAll(
                                async { workoutRepository.syncExercisesFromCloud() },
                                async { mealRepository.syncMealsFromCloud() }
                            )
                        }
                    }

                    // 2. Auth & Status Validation
                    val auth = FirebaseAuth.getInstance()
                    val firebaseUser = auth.currentUser
                    var isAccountValid = firebaseUser != null
                    var isMarkedDeleted = false

                    if (firebaseUser != null) {
                        try {
                            firebaseUser.reload().await()

                            val doc = FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(firebaseUser.uid)
                                .get()
                                .await()

                            if (doc.exists()) {
                                val status = doc.getString("status")
                                if (status == "deleted") {
                                    isMarkedDeleted = true
                                    isAccountValid = false
                                }
                            }
                        } catch (e: Exception) {
                            if (e is FirebaseAuthInvalidUserException) {
                                isAccountValid = false
                            } else if (e is FirebaseNetworkException) {
                                isAccountValid = true // Allow offline access
                            }
                        }
                    }

                    if (isMarkedDeleted || (firebaseUser != null && !isAccountValid)) {
                        auth.signOut()
                        userRepository.deleteAll()
                    }

                    // 3. Navigation Decision
                    val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
                    val isOnboardingFinished = sharedPref.getBoolean("Finished", false)

                    val destination = if (isAccountValid) {
                        // Check if we have local or cloud data to skip onboarding
                        val localUser = userRepository.getInitialUser()

                        if (localUser != null || isOnboardingFinished) {
                            // If local data exists, we proceed to Home
                            if (localUser != null && !localUser.personalizedPlanJson.isNullOrBlank()) {
                                launch(Dispatchers.IO) {
                                    try {
                                        val plan = gson.fromJson(localUser.personalizedPlanJson, WorkoutPlan::class.java)
                                        workoutPlanner.syncPlanToDatabase(plan)
                                    } catch (e: Exception) { }
                                }
                            }
                            R.id.action_splashFragment_to_mainTabsFragment
                        } else {
                            // No local data, try to restore from cloud
                            val restored = userRepository.restoreUserFromCloud()
                            if (restored) {
                                // Mark onboarding as finished locally if we restored a profile
                                sharedPref.edit().putBoolean("Finished", true).apply()
                                R.id.action_splashFragment_to_mainTabsFragment
                            } else {
                                R.id.action_splashFragment_to_viewPagerFragment
                            }
                        }
                    } else {
                        R.id.action_splashFragment_to_viewPagerFragment
                    }

                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime < 2000) delay(2000 - elapsedTime)

                    navigateTo(destination)

                } catch (e: Exception) {
                    Log.e("Splash", "Nav error", e)
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
