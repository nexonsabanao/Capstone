package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.data.model.User
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.databinding.FragmentLoginBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.KeyboardUtil
import com.example.nutriority.planner.WorkoutPlan
import com.example.nutriority.planner.WorkoutPlanner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : BaseBindingFragment<FragmentLoginBinding>(FragmentLoginBinding::inflate) {

    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var mealRepository: MealRepository
    @Inject lateinit var workoutPlanner: WorkoutPlanner
    @Inject lateinit var gson: Gson

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etEmail.doAfterTextChanged { binding.tvError.isVisible = false }
        binding.etPassword.doAfterTextChanged { binding.tvError.isVisible = false }

        binding.btnLogin.setOnClickListener {
            KeyboardUtil.hideKeyboard(requireActivity())
            val email = binding.etEmail.text.toString().lowercase().trim()
            val password = binding.etPassword.text.toString().trim()

            when {
                email.isBlank() -> showError("Email is required")
                password.isBlank() -> showError("Password is required")
                !isValidStudentEmail(email) -> showError("Access restricted: Use student email (xxxx@cvsu.edu.ph)")
                else -> performFirebaseLogin(email, password)
            }
        }

        binding.tvSignUp.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestNext", Bundle())
        }

        binding.tvForgotPassword.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestForgotPassword", Bundle())
        }
    }

    private fun performFirebaseLogin(email: String, password: String) {
        showAuthOverlay(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { 
                checkAccountStatusAndProceed()
            }
            .addOnFailureListener {
                showAuthOverlay(false)
                showError("Invalid email or password.")
            }
    }

    private fun checkAccountStatusAndProceed() {
        val firebaseUser = auth.currentUser ?: return
        val uid = firebaseUser.uid
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Force reload to get latest verification status
                firebaseUser.reload().await()
                
                // 2. Check if email is verified
                if (!firebaseUser.isEmailVerified) {
                    auth.signOut()
                    showAuthOverlay(false)
                    showError("Please verify your email before logging in. Check your CVSU inbox.")
                    return@launch
                }

                // 3. Check Firestore for deleted status
                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()
                
                if (doc.exists()) {
                    val status = doc.getString("status")
                    if (status == "deleted") {
                        auth.signOut()
                        showAuthOverlay(false)
                        showError("This account has been disabled by the administrator.")
                        return@launch
                    }
                }
                
                // Proceed to data restoration or new user creation
                restoreAndProceed()
                
            } catch (e: Exception) {
                auth.signOut()
                showAuthOverlay(false)
                showError("Login verification failed. Please try again.")
            }
        }
    }

    private fun restoreAndProceed() {
        val firebaseUser = auth.currentUser ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Try to restore existing data
                val restored = userRepository.restoreUserFromCloud()
                
                if (restored) {
                    // Existing user - sync their historical data
                    coroutineScope {
                        awaitAll(
                            async { workoutRepository.restoreHistoryFromCloud() },
                            async { mealRepository.restoreMealsFromCloud() }
                        )
                    }
                    userRepository.recalculateUserStats()
                    
                    val localUser = userRepository.getInitialUser()
                    if (localUser != null && !localUser.personalizedPlanJson.isNullOrBlank()) {
                        try {
                            val plan = gson.fromJson(localUser.personalizedPlanJson, WorkoutPlan::class.java)
                            workoutPlanner.syncPlanToDatabase(plan)
                        } catch (e: Exception) { }
                    }
                } else {
                    // New user - First time login after email verification
                    // Create their profile record now
                    val newUser = User(
                        id = 1,
                        email = firebaseUser.email ?: "",
                        status = "active",
                        lastCompletedWorkoutDay = 0
                    )
                    userRepository.insertUser(newUser)
                }

                updateOverlayToSuccess()
                delay(1000)
            } catch (e: Exception) {
                Log.e("Login", "Restoration/Creation error", e)
            } finally {
                showAuthOverlay(false)
                parentFragmentManager.setFragmentResult("navigationRequestWelcome", Bundle())
            }
        }
    }

    private fun showAuthOverlay(show: Boolean) {
        binding.authOverlay.isVisible = show
        if (show) {
            binding.authProgress.isVisible = true
            binding.authCheck.isVisible = false
            binding.authStatusText.text = "Authenticating..."
        }
    }

    private fun updateOverlayToSuccess() {
        binding.authProgress.isVisible = false
        binding.authCheck.isVisible = true
        binding.authStatusText.text = "Logged in successfully"
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.isVisible = true
    }

    private fun isValidStudentEmail(email: String): Boolean = email.endsWith("@cvsu.edu.ph")
}
