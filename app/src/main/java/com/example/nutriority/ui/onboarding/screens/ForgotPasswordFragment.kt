package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.databinding.FragmentForgotPasswordBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import com.example.nutriority.ui.util.KeyboardUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class ForgotPasswordFragment : BaseBindingFragment<FragmentForgotPasswordBinding>(FragmentForgotPasswordBinding::inflate) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Listen for page selection to reset UI when the user navigates to this fragment in ViewPager
        parentFragmentManager.setFragmentResultListener("pageSelected", viewLifecycleOwner) { _, bundle ->
            val position = bundle.getInt("position", -1)
            if (position == 2) { // Position 2 is ForgotPasswordFragment
                resetUI()
            }
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
        }

        binding.etEmail.doAfterTextChanged { binding.tvError.isVisible = false }

        binding.btnSendCodeInitial.setOnClickListener {
            KeyboardUtil.hideKeyboard(requireActivity())
            val email = binding.etEmail.text.toString().lowercase().trim()

            if (email.isBlank()) {
                showError("Please enter your email address")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Please enter a valid email address")
                return@setOnClickListener
            }

            checkAccountAndProceed(email)
        }

        binding.btnDone.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
        }
    }

    override fun onResume() {
        super.onResume()
        resetUI()
    }

    private fun resetUI() {
        if (view != null) {
            binding.layoutStepEmail.visibility = View.VISIBLE
            binding.layoutStepSuccess.visibility = View.GONE
            binding.tvError.isVisible = false
            binding.etEmail.text?.clear()
            binding.btnSendCodeInitial.isEnabled = true
            binding.btnSendCodeInitial.text = "SEND RESET LINK"
            binding.tvSubtitle.text = "Enter your email address and we'll send you a link to reset your password."
        }
    }

    private fun checkAccountAndProceed(email: String) {
        binding.btnSendCodeInitial.isEnabled = false
        binding.btnSendCodeInitial.text = "VERIFYING..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val query = FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    val doc = query.documents[0]
                    val status = doc.getString("status")
                    if (status == "deleted") {
                        showError("This account has been disabled. Please contact the administrator.")
                        binding.btnSendCodeInitial.isEnabled = true
                        binding.btnSendCodeInitial.text = "SEND RESET LINK"
                        return@launch
                    }
                    sendPasswordResetEmail(email)
                } else {
                    val signInMethods = auth.fetchSignInMethodsForEmail(email).await().signInMethods
                    if (signInMethods.isNullOrEmpty()) {
                        showError("This email is not registered with a Nutriority account.")
                        binding.btnSendCodeInitial.isEnabled = true
                        binding.btnSendCodeInitial.text = "SEND RESET LINK"
                    } else {
                        sendPasswordResetEmail(email)
                    }
                }
            } catch (e: Exception) {
                sendPasswordResetEmail(email)
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        binding.btnSendCodeInitial.text = "SENDING..."

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (context == null || view == null) return@addOnCompleteListener
                
                binding.btnSendCodeInitial.isEnabled = true
                binding.btnSendCodeInitial.text = "SEND RESET LINK"

                if (task.isSuccessful) {
                    binding.layoutStepEmail.visibility = View.GONE
                    binding.layoutStepSuccess.visibility = View.VISIBLE
                    binding.tvSubtitle.text = "A password reset link has been sent to $email. Please check your inbox or SPAM folder."
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
}
