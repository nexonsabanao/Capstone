package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.data.model.User
import com.example.nutriority.data.repository.UserRepository
import com.example.nutriority.databinding.FragmentSignUpBinding
import com.example.nutriority.ui.util.KeyboardUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var userRepository: UserRepository

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var pendingEmail: String? = null
    private var pendingPass: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetState()

        parentFragmentManager.setFragmentResultListener("pageSelected", viewLifecycleOwner) { _, bundle ->
            val position = bundle.getInt("position", -1)
            if (position == 1) resetState()
        }

        binding.btnBack.setOnClickListener { handleBackAction() }

        binding.etEmail.doAfterTextChanged { binding.tvError.isVisible = false }
        binding.etPassword.doAfterTextChanged { binding.tvError.isVisible = false }
        binding.etConfirmPassword.doAfterTextChanged { binding.tvError.isVisible = false }

        binding.btnSignUp.setOnClickListener { 
            KeyboardUtil.hideKeyboard(requireActivity())
            validateInputsAndProceed() 
        }
        
        // This button now triggers the official Firebase Verification Link
        binding.btnSendCode.setOnClickListener { 
            KeyboardUtil.hideKeyboard(requireActivity())
            performRegistrationAndSendLink() 
        }
        
        binding.btnVerify.setOnClickListener {
            // After sending link, user just needs to go to login
            parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
        }
    }

    private fun resetState() {
        pendingEmail = null
        pendingPass = null
        if (_binding != null) {
            binding.etEmail.text?.clear()
            binding.etPassword.text?.clear()
            binding.etConfirmPassword.text?.clear()
            binding.tvError.isVisible = false
            binding.layoutVerification.isVisible = false
            binding.inputContainer.isVisible = true
            binding.tvTitle.text = "Create Account"
            binding.tvSubtitle.isVisible = true
            binding.btnSignUp.isEnabled = true
            binding.btnSignUp.text = "CONTINUE"
        }
    }

    private fun handleBackAction() {
        if (binding.layoutVerification.isVisible) {
            binding.layoutVerification.isVisible = false
            binding.inputContainer.isVisible = true
            binding.tvTitle.text = "Create Account"
            binding.tvSubtitle.isVisible = true
            binding.tvError.isVisible = false
        } else {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }
    }

    private fun validateInputsAndProceed() {
        val email = binding.etEmail.text.toString().lowercase().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()

        when {
            email.isBlank() -> showError("Please enter your CVSU email")
            !isValidStudentEmail(email) -> showError("Invalid: Use CVSU student email (tmc.xxxx@cvsu.edu.ph)")
            password.length < 8 -> showError("Password must be at least 8 characters")
            !password.any { it.isUpperCase() } -> showError("Password must contain an upper case character")
            !password.any { it.isDigit() } -> showError("Password must contain a numeric character")
            password != confirmPass -> showError("Passwords do not match")
            else -> checkEmailAvailability(email, password)
        }
    }

    private fun checkEmailAvailability(email: String, pass: String) {
        binding.btnSignUp.isEnabled = false
        binding.btnSignUp.text = "CHECKING EMAIL..."

        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (_binding == null) return@addOnCompleteListener

            binding.btnSignUp.isEnabled = true
            binding.btnSignUp.text = "CONTINUE"

            if (task.isSuccessful) {
                val methods = task.result?.signInMethods ?: emptyList()
                if (methods.isEmpty()) {
                    pendingEmail = email
                    pendingPass = pass
                    
                    // Switch to Verification Link View
                    binding.inputContainer.isVisible = false
                    binding.layoutVerification.isVisible = true
                    binding.tvTitle.text = "Verify Email"
                    binding.tvSubtitle.isVisible = false
                    binding.tvError.isVisible = false
                    
                    // UI adjustment for Link Flow
                    binding.pinInputLayout.isVisible = false
                    binding.tvVerifySubtitle.text = "Click below to register and send a verification link to your student email."
                    binding.btnSendCode.text = "SEND VERIFICATION LINK"
                    binding.btnVerify.isVisible = false
                } else {
                    showError("Email is already registered. Please login.")
                }
            } else {
                showError("Error: ${task.exception?.message}")
            }
        }
    }

    private fun performRegistrationAndSendLink() {
        val email = pendingEmail ?: return
        val pass = pendingPass ?: return

        binding.btnSendCode.isEnabled = false
        binding.btnSendCode.text = "REGISTERING..."

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                        if (_binding == null) return@addOnCompleteListener
                        
                        if (verifyTask.isSuccessful) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                val newUser = User(
                                    id = 1,
                                    gender = "",
                                    birthDate = null,
                                    heightCm = 0.0,
                                    weightKg = 0.0,
                                    unitSystem = "METRIC",
                                    activityLevel = "",
                                    goal = "",
                                    preferredDiet = "",
                                    excludedIngredients = emptyList(),
                                    lastCompletedWorkoutDay = 0
                                )
                                userRepository.insertUser(newUser)
                                
                                // Show Success UI
                                binding.tvVerifySubtitle.text = "A verification link has been sent to $email. Please check your inbox and verify your account before logging in."
                                binding.btnSendCode.isVisible = false
                                binding.btnVerify.isVisible = true
                                binding.btnVerify.text = "GO TO LOGIN"
                                Toast.makeText(requireContext(), "Verification email sent!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            showError("Failed to send link: ${verifyTask.exception?.message}")
                            binding.btnSendCode.isEnabled = true
                            binding.btnSendCode.text = "RETRY"
                        }
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        showError("This email is already in use.")
                    } else {
                        showError("Registration failed: ${exception?.message}")
                    }
                    binding.btnSendCode.isEnabled = true
                    binding.btnSendCode.text = "SEND VERIFICATION LINK"
                }
            }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true
    }

    private fun isValidStudentEmail(email: String): Boolean {
        val lowerEmail = email.lowercase().trim()
        return lowerEmail.startsWith("tmc.") && lowerEmail.endsWith("@cvsu.edu.ph")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
