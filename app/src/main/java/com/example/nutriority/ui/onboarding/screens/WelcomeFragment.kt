package com.example.nutriority.ui.onboarding.screens

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.databinding.FragmentWelcomeBinding
import com.example.nutriority.ui.util.BaseBindingFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeFragment : BaseBindingFragment<FragmentWelcomeBinding>(FragmentWelcomeBinding::inflate) {

    private val userViewModel: UserViewModel by activityViewModels()

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
            // Check if user has already completed onboarding (has a plan)
            val user = userViewModel.user.value
            if (user != null && !user.personalizedPlanJson.isNullOrBlank()) {
                // RETURNER: Skip onboarding and go straight to the main app dashboard
                findNavController().navigate(R.id.action_viewPagerFragment_to_mainTabsFragment)
            } else {
                // NEW USER: Continue with the onboarding flow
                setFragmentResult("navigationRequestNext", Bundle())
            }
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

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Terms and Services")
            .setMessage(termsAndServicesText)
            .setPositiveButton("OK", null)
            .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
    }
}
