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
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@Suppress("DEPRECATION")
@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var userRepository: UserRepository

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

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
            validateAndRegister() 
        }
        
        binding.btnVerify.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
        }
    }

    private fun resetState() {
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
            resetState()
        } else {
            parentFragmentManager.setFragmentResult("navigationRequestPrevious", Bundle())
        }
    }

    private fun validateAndRegister() {
        val email = binding.etEmail.text.toString().lowercase().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()

        when {
            email.isBlank() -> showError("Please enter your CVSU email")
            !isValidStudentEmail(email) -> showError("Invalid: Use CVSU student email (xxxx@cvsu.edu.ph)")
            password.length < 8 -> showError("Password must be at least 8 characters")
            !password.any { it.isUpperCase() } -> showError("Password must contain an upper case character")
            !password.any { it.isDigit() } -> showError("Password must contain a numeric character")
            password != confirmPass -> showError("Passwords do not match")
            else -> checkDeletedStatusAndRegister(email, password)
        }
    }

    private fun checkDeletedStatusAndRegister(email: String, pass: String) {
        binding.btnSignUp.isEnabled = false
        binding.btnSignUp.text = "VERIFYING..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Pre-check Firestore for deleted status
                val query = FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    val status = query.documents[0].getString("status")
                    if (status == "deleted") {
                        showError("This email is associated with a disabled account. Please contact support.")
                        binding.btnSignUp.isEnabled = true
                        binding.btnSignUp.text = "CONTINUE"
                        return@launch
                    }
                }
                performRegistration(email, pass)
            } catch (e: Exception) {
                performRegistration(email, pass)
            }
        }
    }

    private fun performRegistration(email: String, pass: String) {
        binding.btnSignUp.isEnabled = false
        binding.btnSignUp.text = "REGISTERING..."

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (_binding == null) return@addOnCompleteListener
                
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                        if (_binding == null) return@addOnCompleteListener
                        
                        if (verifyTask.isSuccessful) {
                            // Account creation in Firestore removed. 
                            // It will be created in LoginFragment after email verification.
                            binding.inputContainer.isVisible = false
                            binding.layoutVerification.isVisible = true
                            binding.tvTitle.text = "Verify Email"
                            binding.tvSubtitle.isVisible = false
                            binding.tvVerifySubtitle.text = "Welcome to Nutriority! A verification link has been sent to $email.\n\nPlease check your CVSU inbox (and spam folder) to verify your account."
                            
                            binding.btnSendCode.isVisible = false 
                            binding.btnVerify.isVisible = true
                            binding.btnVerify.text = "GO TO LOGIN"
                            
                            Toast.makeText(requireContext(), "Verification email sent!", Toast.LENGTH_LONG).show()
                        } else {
                            showError("Failed to send verification link: ${verifyTask.exception?.message}")
                            binding.btnSignUp.isEnabled = true
                            binding.btnSignUp.text = "RETRY"
                        }
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        // Double check if this collision is with a deleted account
                        viewLifecycleOwner.lifecycleScope.launch {
                            val query = FirebaseFirestore.getInstance()
                                .collection("users")
                                .whereEqualTo("email", email)
                                .limit(1)
                                .get()
                                .await()
                            
                            if (!query.isEmpty && query.documents[0].getString("status") == "deleted") {
                                showError("This account has been disabled. Please contact support.")
                            } else {
                                showError("This email is already in use. Try logging in.")
                            }
                            binding.btnSignUp.isEnabled = true
                            binding.btnSignUp.text = "CONTINUE"
                        }
                    } else {
                        showError("Registration failed: ${exception?.message}")
                        binding.btnSignUp.isEnabled = true
                        binding.btnSignUp.text = "CONTINUE"
                    }
                }
            }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true
    }

    private fun isValidStudentEmail(email: String): Boolean = email.lowercase().trim().endsWith("@cvsu.edu.ph")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
