package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.data.model.User
import com.example.nutriority.data.repository.MealRepository
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.data.repository.WorkoutRepository
import com.example.nutriority.databinding.FragmentLoginBinding
import com.google.firebase.auth.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var mealRepository: MealRepository

    private val userViewModel: UserViewModel by activityViewModels()
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etEmail.doAfterTextChanged { binding.tvError.isVisible = false }
        binding.etPassword.doAfterTextChanged { binding.tvError.isVisible = false }

        binding.btnLogin.setOnClickListener {
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
                withTimeout(15000) {
                    userRepository.restoreUserFromCloud()
                }
                coroutineScope {
                    awaitAll(
                        async { workoutRepository.restoreHistoryFromCloud() },
                        async { mealRepository.restoreMealsFromCloud() }
                    )
                }
                
                // Show success state
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
        if (_binding == null) return
        binding.authOverlay.isVisible = show
        if (show) {
            binding.authProgress.isVisible = true
            binding.authCheck.isVisible = false
            binding.authStatusText.text = "Authenticating..."
        }
    }

    private fun updateOverlayToSuccess() {
        if (_binding == null) return
        binding.authProgress.isVisible = false
        binding.authCheck.isVisible = true
        binding.authStatusText.text = "Log in successfully"
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.isVisible = true
    }

    private fun isValidStudentEmail(email: String): Boolean {
        return email.startsWith("tmc.") && email.endsWith("@cvsu.edu.ph")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
