package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.nutriority.databinding.FragmentForgotPasswordBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.KeyboardUtil
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : BaseBindingFragment<FragmentForgotPasswordBinding>(FragmentForgotPasswordBinding::inflate) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
        }

        binding.etEmail.doAfterTextChanged { binding.tvError.isVisible = false }

        binding.btnSendCodeInitial.setOnClickListener {
            KeyboardUtil.hideKeyboard(requireActivity())
            val email = binding.etEmail.text.toString().trim()

            if (email.isBlank()) {
                showError("Please enter your email address")
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                showError("Please enter a valid CVSU student email (xxxx@cvsu.edu.ph)")
                return@setOnClickListener
            }

            verifyAndSendResetEmail(email)
        }

        binding.btnDone.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
        }
    }

    private fun verifyAndSendResetEmail(email: String) {
        binding.btnSendCodeInitial.isEnabled = false
        binding.btnSendCodeInitial.text = "VERIFYING..."

        // Check if the account exists first
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { checkTask ->
            if (context == null) return@addOnCompleteListener

            if (checkTask.isSuccessful) {
                val signInMethods = checkTask.result?.signInMethods
                
                if (signInMethods.isNullOrEmpty()) {
                    // No account found for this email
                    binding.btnSendCodeInitial.isEnabled = true
                    binding.btnSendCodeInitial.text = "SEND RESET LINK"
                    showError("This email is not registered with a Nutriority account.")
                } else {
                    // Account exists, proceed to send reset email
                    sendPasswordResetEmail(email)
                }
            } else {
                // If checking fails (e.g. network error), try sending anyway as a fallback
                sendPasswordResetEmail(email)
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        binding.btnSendCodeInitial.text = "SENDING..."

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (context == null) return@addOnCompleteListener
                
                binding.btnSendCodeInitial.isEnabled = true
                binding.btnSendCodeInitial.text = "SEND RESET LINK"

                if (task.isSuccessful) {
                    binding.layoutStepEmail.visibility = View.GONE
                    binding.layoutStepSuccess.visibility = View.VISIBLE
                    binding.tvSubtitle.text = "A password reset link has been sent to $email. Please check your inbox or SPAM folder and follow the instructions."
                    Toast.makeText(requireContext(), "Reset link sent!", Toast.LENGTH_LONG).show()
                } else {
                    showError("Error: ${task.exception?.message}")
                }
            }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true
    }

    private fun isValidEmail(email: String): Boolean {
        val lowerEmail = email.lowercase().trim()
        return lowerEmail.endsWith("@cvsu.edu.ph")
    }
}
