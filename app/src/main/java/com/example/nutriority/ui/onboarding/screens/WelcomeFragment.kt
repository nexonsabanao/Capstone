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
            Welcome to Nutriority! By using this app, you agree to the following terms. Please read them carefully.

            1. Who Can Use This App
            Nutriority is exclusively available to currently enrolled students of Cavite State University – Trece Martires City Campus. You must use your valid CvSU credentials or information to register and access the app.

            2. Health Disclaimer
            ⚠ IMPORTANT: Nutriority is NOT recommended for users with health problems or injuries.

            The workout plans and nutrition content in this app are for general wellness purposes only and do not constitute medical advice. Do not use this app if you have any of the following:
            - Pre-existing medical conditions (e.g., heart disease, diabetes, hypertension)
            - Current or recent injuries (e.g., joint, muscle, or spinal injuries)
            - Any condition for which a doctor has advised you to avoid physical activity
            - Pregnancy or postpartum recovery

            If you are unsure whether this app is safe for you, please consult a licensed healthcare professional before use. Stop using the app immediately and seek medical attention if you feel pain, dizziness, or discomfort.

            3. User Responsibilities
            By using Nutriority, you agree to:
            - Provide honest and accurate information about yourself
            - Use the app only for personal health and fitness purposes
            - Not share your account with others
            - Not misuse or attempt to damage the app or its data

            4. Privacy
            Any personal information you provide (such as your name, age, and health data) will only be used to operate and improve Nutriority. Your data will not be shared with third parties outside of CvSU without your consent.

            5. Limitation of Liability
            Nutriority and its developers are not liable for any injury, health issue, or damages that may result from using the app. You use the app at your own risk.

            6. Changes to These Terms
            We may update these Terms from time to time. Continued use of the app after any changes means you accept the updated Terms.

            7. Contact
            For questions or concerns, please reach out to the Nutriority team through your CvSU campus channels.

            By using Nutriority, you confirm that you have read and agree to these Terms and Conditions.
            """.trimIndent()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Terms and Conditions")
            .setMessage(termsAndServicesText)
            .setPositiveButton("OK", null)
            .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
    }
}
