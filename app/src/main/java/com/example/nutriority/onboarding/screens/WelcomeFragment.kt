package com.example.nutriority.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nutriority.BaseFragment
import com.example.nutriority.utils.applySystemBarsInsets
import androidx.fragment.app.setFragmentResult
import com.example.nutriority.databinding.FragmentWelcomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.nutriority.utils.applySystemBarsInsets

class WelcomeFragment : BaseFragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.acceptButton.isEnabled = false
        binding.acceptButton.alpha = 0.4f

        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.acceptButton.isEnabled = isChecked
            binding.acceptButton.alpha = if (isChecked) 1.0f else 0.4f
        }

        binding.termsLink.setOnClickListener {
            showTermsDialog()
        }

        binding.acceptButton.setOnClickListener {
            setFragmentResult("navigationRequestNext", Bundle())
        }
    }

    private fun showTermsDialog() {
        val termsAndServicesText = """
            Welcome to Nutriority.
            
            By using our app, you agree to these terms. Please read them carefully.
            
            1. Using our Services
            You must follow any policies made available to you within the Services. Don't misuse our Services. For example, don’t interfere with our Services or try to access them using a method other than the interface and the instructions that we provide.
            
            2. Your Nutriority Account
            You may need a Nutriority Account in order to use some of our Services. You may create your own Nutriority Account, or your Nutriority Account may be assigned to you by an administrator, such as your employer or educational institution.
            
            3. Privacy and Copyright Protection
            Nutriority’s privacy policies explain how we treat your personal data and protect your privacy when you use our Services.
            """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Terms and Services")
            .setMessage(termsAndServicesText)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
