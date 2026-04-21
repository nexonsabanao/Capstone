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
import androidx.core.content.edit

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
                    // 1. Initial Library Sync - Ensure both exercises and meals are present
                    val hasExercises = withContext(Dispatchers.IO) {
                        workoutRepository.getAllExercises().first().isNotEmpty()
                    }
                    val hasMeals = withContext(Dispatchers.IO) {
                        mealRepository.getAllMealsList().isNotEmpty()
                    }

                    if (!hasExercises || !hasMeals) {
                        coroutineScope {
                            val syncTasks = mutableListOf<Deferred<Unit>>()
                            if (!hasExercises) {
                                syncTasks.add(async { workoutRepository.syncExercisesFromCloud() })
                            }
                            if (!hasMeals) {
                                syncTasks.add(async { mealRepository.syncMealsFromCloud() })
                            }
                            syncTasks.awaitAll()
                        }
                    }

                    // 2. Auth & Status Validation
                    val auth = FirebaseAuth.getInstance()
                    val firebaseUser = auth.currentUser
                    var isAccountValid = firebaseUser != null
                    var isMarkedDeleted = false

                    // BUG FIX: Check local status first to catch deleted accounts immediately (offline or cached)
                    val localUser = withContext(Dispatchers.IO) { userRepository.getInitialUser() }
                    if (localUser?.status == "deleted") {
                        isMarkedDeleted = true
                        isAccountValid = false
                    }

                    if (firebaseUser != null && !isMarkedDeleted) {
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
                            } else {
                                // BUG FIX: If document doesn't exist in Firestore, treat as invalid/deleted
                                isAccountValid = false
                            }
                        } catch (e: Exception) {
                            if (e is FirebaseAuthInvalidUserException) {
                                isAccountValid = false
                            } else if (e is FirebaseNetworkException) {
                                // Offline - rely on local status (already checked above)
                                isAccountValid = !isMarkedDeleted
                            }
                        }
                    }

                    if (isMarkedDeleted || (firebaseUser != null && !isAccountValid)) {
                        auth.signOut()
                        withContext(Dispatchers.IO) { userRepository.deleteAll() }
                        
                        // BUG FIX: Reset onboarding flag so they don't skip to Home on next login attempt
                        requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE).edit {
                            putBoolean("Finished", false)
                        }
                        isAccountValid = false
                    }

                    // 3. Navigation Decision
                    val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
                    val isOnboardingFinished = sharedPref.getBoolean("Finished", false)

                    val destination = if (isAccountValid) {
                        // Check local user again (might have been cleared above)
                        val currentUser = withContext(Dispatchers.IO) { userRepository.getInitialUser() }

                        if (currentUser != null || isOnboardingFinished) {
                            // If local data exists, we proceed to Home
                            if (currentUser != null && !currentUser.personalizedPlanJson.isNullOrBlank()) {
                                launch(Dispatchers.IO) {
                                    try {
                                        val plan = gson.fromJson(currentUser.personalizedPlanJson, WorkoutPlan::class.java)
                                        workoutPlanner.syncPlanToDatabase(plan)
                                    } catch (_: Exception) { }
                                }
                            }
                            R.id.action_splashFragment_to_mainTabsFragment
                        } else {
                            // No local data, try to restore from cloud
                            val restored = userRepository.restoreUserFromCloud()
                            if (restored) {
                                // Mark onboarding as finished locally if we restored a profile
                                sharedPref.edit { putBoolean("Finished", true) }
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
