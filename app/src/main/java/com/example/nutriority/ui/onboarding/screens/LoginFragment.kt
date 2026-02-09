package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.databinding.FragmentLoginBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.KeyboardUtil
import com.example.nutriority.planner.WorkoutPlan
import com.example.nutriority.planner.WorkoutPlanner
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
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
                !isValidStudentEmail(email) -> showError("Access restricted: Use student email (tmc.xxxx@cvsu.edu.ph)")
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
            .addOnSuccessListener { restoreAndProceed() }
            .addOnFailureListener {
                showAuthOverlay(false)
                showError("Invalid email or password.")
            }
    }

    private fun restoreAndProceed() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                coroutineScope {
                    awaitAll(
                        async { userRepository.restoreUserFromCloud() },
                        async { workoutRepository.restoreHistoryFromCloud() },
                        async { mealRepository.restoreMealsFromCloud() }
                    )
                }
                
                // "TRICK": Inflate the restored plan into the workout DB silently
                val user = userRepository.getInitialUser()
                if (user != null && !user.personalizedPlanJson.isNullOrBlank()) {
                    try {
                        val plan = gson.fromJson(user.personalizedPlanJson, WorkoutPlan::class.java)
                        workoutPlanner.syncPlanToDatabase(plan)
                    } catch (e: Exception) { }
                }

                updateOverlayToSuccess()
                delay(1500)
            } catch (e: Exception) {
                Log.e("Login", "Restore error", e)
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

    private fun isValidStudentEmail(email: String): Boolean = email.startsWith("tmc.") && email.endsWith("@cvsu.edu.ph")
}
