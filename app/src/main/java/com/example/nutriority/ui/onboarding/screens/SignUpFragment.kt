package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Random
import javax.inject.Inject

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var userRepository: UserRepository

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }

    private var pendingEmail: String? = null
    private var pendingPass: String? = null
    private var pendingPin: String? = null

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

        binding.btnSignUp.setOnClickListener { validateInputsAndProceed() }
        binding.btnSendCode.setOnClickListener { pendingEmail?.let { sendVerificationPin(it) } }
        binding.btnVerify.setOnClickListener { verifyPinAndRegister() }
    }

    private fun resetState() {
        pendingEmail = null
        pendingPass = null
        pendingPin = null
        if (_binding != null) {
            binding.etEmail.text?.clear()
            binding.etPassword.text?.clear()
            binding.etConfirmPassword.text?.clear()
            binding.etPin.text?.clear()
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
            else -> checkEmailAndTransition(email, password)
        }
    }

    private fun checkEmailAndTransition(email: String, pass: String) {
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
                    binding.inputContainer.isVisible = false
                    binding.layoutVerification.isVisible = true
                    binding.tvTitle.text = "Verification"
                    binding.tvSubtitle.isVisible = false
                    binding.tvError.isVisible = false
                    binding.btnSendCode.text = "SEND"
                } else {
                    showError("Email is already registered. Please login.")
                }
            } else {
                showError("Error: ${task.exception?.message}")
            }
        }
    }

    private fun sendVerificationPin(email: String) {
        binding.btnSendCode.isEnabled = false
        binding.btnSendCode.text = "..."

        // Generate 6-digit PIN
        val pin = String.format("%06d", Random().nextInt(999999))
        pendingPin = pin

        val data = hashMapOf(
            "email" to email,
            "pin" to pin
        )

        // Call Firebase Cloud Function
        functions.getHttpsCallable("sendVerificationPin")
            .call(data)
            .addOnSuccessListener { result ->
                Toast.makeText(requireContext(), "Verification code sent!", Toast.LENGTH_SHORT).show()
                binding.tvVerifySubtitle.text = "Verification code sent to $email (Valid for 5 min)"
                binding.btnSendCode.isEnabled = true
                binding.btnSendCode.text = "RESEND"
            }
            .addOnFailureListener { e ->
                showError("Failed to send PIN: ${e.message}")
                binding.btnSendCode.isEnabled = true
                binding.btnSendCode.text = "SEND"
            }
    }

    private fun verifyPinAndRegister() {
        val enteredPin = binding.etPin.text.toString().trim()
        val email = pendingEmail ?: return
        val pass = pendingPass ?: return
        val pin = pendingPin ?: return

        if (enteredPin.isEmpty()) {
            showError("Please enter the verification code")
            return
        }

        binding.btnVerify.isEnabled = false
        binding.btnVerify.text = "VERIFYING..."

        when {
            enteredPin != pin -> {
                showError("Incorrect verification code")
                binding.btnVerify.isEnabled = true
                binding.btnVerify.text = "VERIFY & REGISTER"
            }
            else -> {
                performRegistration()
            }
        }
    }

    private fun performRegistration() {
        val email = pendingEmail ?: return
        val pass = pendingPass ?: return

        binding.btnVerify.text = "REGISTERING..."

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val newUser = User(
                            id = 1,
                            gender = "",
                            age = null,
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
                        parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        showError("This email is already in use.")
                    } else {
                        showError("Registration failed: ${exception?.message}")
                    }
                    binding.btnVerify.isEnabled = true
                    binding.btnVerify.text = "VERIFY & REGISTER"
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
