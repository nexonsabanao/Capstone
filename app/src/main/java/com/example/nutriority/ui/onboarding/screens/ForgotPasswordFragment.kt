package com.example.nutriority.ui.onboarding.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
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

        resetState()

        parentFragmentManager.setFragmentResultListener("pageSelected", viewLifecycleOwner) { _, bundle ->
            val position = bundle.getInt("position", -1)
            if (position == 2) { 
                resetState()
            }
        }

        binding.btnBack.setOnClickListener {
            handleBackAction()
        }

        binding.etEmail.doAfterTextChanged { hideError() }

        binding.btnSendCodeInitial.setOnClickListener {
            validateEmailAndSendLink()
        }

        binding.btnDone.setOnClickListener {
            parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
        }
    }

    private fun resetState() {
        targetEmail = null
        if (_binding != null) {
            binding.etEmail.text?.clear()
            hideError()
            showStepEmail()
        }
    }

    private fun handleBackAction() {
        if (binding.layoutStepSuccess.isVisible) {
            resetState()
        } else {
            parentFragmentManager.setFragmentResult("navigationRequestLogin", Bundle())
        }
    }

    private fun validateEmailAndSendLink() {
        val email = binding.etEmail.text.toString().lowercase().trim()
        if (email.isEmpty() || !isValidStudentEmail(email)) {
            showError("Please enter a valid CVSU student email")
            return
        }

        binding.btnSendCodeInitial.isEnabled = false
        binding.btnSendCodeInitial.text = "SENDING..."

        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (_binding == null) return@addOnCompleteListener
            
            if (task.isSuccessful) {
                targetEmail = email
                showStepSuccess(email)
            } else {
                val errorMsg = task.exception?.message ?: "Unknown error"
                showError("Firebase Error: $errorMsg")
                binding.btnSendCodeInitial.isEnabled = true
                binding.btnSendCodeInitial.text = "SEND RESET LINK"
            }
        }
    }

    private fun showStepEmail() {
        binding.layoutStepEmail.isVisible = true
        binding.layoutStepSuccess.isVisible = false
        binding.tvTitle.text = "Reset Password"
        binding.tvSubtitle.text = "Enter your CVSU email to receive a reset link"
        binding.tvSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
        binding.btnSendCodeInitial.isEnabled = true
        binding.btnSendCodeInitial.text = "SEND RESET LINK"
    }

    private fun showStepSuccess(email: String) {
        binding.layoutStepEmail.isVisible = false
        binding.layoutStepSuccess.isVisible = true
        binding.tvTitle.text = "Success!"
        binding.tvSubtitle.text = "A password reset link has been sent to $email. Please check your inbox."
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
