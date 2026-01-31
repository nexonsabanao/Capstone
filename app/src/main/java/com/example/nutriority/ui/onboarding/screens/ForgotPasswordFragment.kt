package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var generatedPin: String? = null
    private var targetEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reset state whenever the view is created
        resetState()

        // Also reset state whenever this page is selected in the ViewPager
        parentFragmentManager.setFragmentResultListener("pageSelected", viewLifecycleOwner) { _, bundle ->
            val position = bundle.getInt("position", -1)
            if (position == 2) { // Position of ForgotPasswordFragment
                resetState()
            }
        }

        binding.btnBack.setOnClickListener {
            handleBackAction()
        }

        binding.etEmail.doAfterTextChanged { hideError() }
        binding.etPin.doAfterTextChanged { hideError() }

        binding.btnSendCodeInitial.setOnClickListener {
            validateEmailAndProceed()
        }

        binding.btnVerifyPin.setOnClickListener {
            val enteredPin = binding.etPin.text.toString().trim()
            if (generatedPin != null && enteredPin == generatedPin) {
                // Pin is correct, trigger the official Firebase password reset
                triggerOfficialReset()
            } else {
                showError("Incorrect verification code")
            }
        }

        binding.btnSendCode.setOnClickListener {
            targetEmail?.let { sendResetPin(it) }
        }

        binding.btnDone.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
        }
    }

    private fun resetState() {
        generatedPin = null
        targetEmail = null
        if (_binding != null) {
            binding.etEmail.text?.clear()
            binding.etPin.text?.clear()
            hideError()
            showStepEmail()
        }
    }

    private fun handleBackAction() {
        when {
            binding.layoutStepPin.isVisible -> showStepEmail()
            binding.layoutStepSuccess.isVisible -> resetState()
            else -> parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
        }
    }

    private fun validateEmailAndProceed() {
        val email = binding.etEmail.text.toString().lowercase().trim()
        if (email.isEmpty() || !isValidStudentEmail(email)) {
            showError("Please enter a valid CVSU student email")
            return
        }

        targetEmail = email
        sendResetPin(email)
        showStepPin()
    }

    private fun sendResetPin(email: String) {
        binding.btnSendCode.isEnabled = false
        binding.btnSendCode.text = "WAIT"
        
        generatedPin = String.format("%06d", Random().nextInt(999999))
        Log.d("ForgotPassword", "Reset PIN for $email: $generatedPin")
        
        binding.tvSubtitle.text = "A verification code has been generated for $email"
        
        lifecycleScope.launch {
            delay(2000)
            if (_binding != null) {
                binding.btnSendCode.isEnabled = true
                binding.btnSendCode.text = "RESEND"
            }
        }
    }

    private fun triggerOfficialReset() {
        val email = targetEmail ?: return
        
        binding.btnVerifyPin.isEnabled = false
        binding.btnVerifyPin.text = "VERIFYING..."

        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showStepSuccess()
            } else {
                val errorMsg = task.exception?.message ?: "Unknown error"
                showError("Firebase Error: $errorMsg")
                Log.e("ForgotPassword", "Reset failed: $errorMsg")
                binding.btnVerifyPin.isEnabled = true
                binding.btnVerifyPin.text = "VERIFY PIN"
            }
        }
    }

    private fun showStepEmail() {
        binding.layoutStepEmail.isVisible = true
        binding.layoutStepPin.isVisible = false
        binding.layoutStepSuccess.isVisible = false
        binding.tvTitle.text = "Reset Password"
        binding.tvSubtitle.text = "Enter your CVSU email to receive a reset code"
        binding.tvSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
    }

    private fun showStepPin() {
        binding.layoutStepEmail.isVisible = false
        binding.layoutStepPin.isVisible = true
        binding.layoutStepSuccess.isVisible = false
        binding.tvTitle.text = "Verification"
        binding.btnSendCode.text = "SEND"
    }

    private fun showStepSuccess() {
        binding.layoutStepEmail.isVisible = false
        binding.layoutStepPin.isVisible = false
        binding.layoutStepSuccess.isVisible = true
        binding.tvTitle.text = "Success!"
        binding.tvSubtitle.text = "Identity verified"
        binding.tvSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
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
