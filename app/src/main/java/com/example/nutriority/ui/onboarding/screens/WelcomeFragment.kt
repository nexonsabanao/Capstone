package com.example.nutriority.ui.onboarding.screens

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
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

        binding.btnInfo.setOnClickListener {
            showStudyInfoDialog()
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

    private fun showStudyInfoDialog() {
        val infoText = """
            <b>Nutriority</b> is a cutting-edge, personalized fitness and nutrition companion owned and operated by <b>Xfactor Fitness Gym</b>. Our mission is to transform lives by providing expert-level guidance that is accessible to everyone.<br><br>
            <b>Key Features:</b><br>
            • <b>Personalized Workout Plans:</b> Tailored to your fitness level, goals, and available equipment.<br>
            • <b>Nutrition Tracking:</b> Smart meal logging and diet plans based on your preferences.<br>
            • <b>Progress Monitoring:</b> Track your body metrics and workout consistency over time.<br>
            • <b>Expert Guidance:</b> Science-backed routines designed to maximize results and minimize injury risk.<br><br>
            Whether you are looking to lose weight, build muscle, or maintain a healthy lifestyle, <b>Nutriority</b> provides the tools and motivation you need to succeed.<br><br>
            
            <b>Research & Development:</b><br>
            <b>Study Conducted at:</b><br>
            XFactor Fitness Gym Trece<br><br>
            
            <b>Professional Guidance:</b><br>
            • Headcoach: Skylove Panaligan<br>
            • Nutritionist: Mark Anthony Rimando<br><br>
            
            <b>Creators & Researchers:</b><br>
            • Nexon Jr. Y. Sabañao<br>
            • Kenneth Ian B. Benedicto<br>
            • Ivan A. Pamaran<br><br>
            
            <i>Version 1.0.0</i>
        """.trimIndent()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("About Nutriority")
            .setMessage(HtmlCompat.fromHtml(infoText, HtmlCompat.FROM_HTML_MODE_COMPACT))
            .setPositiveButton("Close", null)
            .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
    }

    private fun showTermsDialog() {
        val termsAndServicesText = """
            Welcome to <b>Nutriority</b>! By using this app, you agree to the following terms. Please read them carefully.<br><br>
            <b>1. Who Can Use This App</b><br>
            Nutriority is a service provided by <b>Xfactor Fitness Gym</b>. By registering, you agree to provide accurate information to receive the best possible fitness and nutrition guidance.<br><br>
            <b>2. Health Disclaimer</b><br>
            ⚠ <b>IMPORTANT:</b> Nutriority is <b>NOT recommended</b> for users with significant health problems or injuries without medical clearance.<br><br>
            The workout plans and nutrition content in this app are for general wellness purposes only and do not constitute medical advice. Do not use this app if you have any of the following:<br>
            • Pre-existing medical conditions (e.g., heart disease, diabetes, hypertension)<br>
            • Current or recent injuries (e.g., joint, muscle, or spinal injuries)<br>
            • Any condition for which a doctor has advised you to avoid physical activity<br>
            • Pregnancy or postpartum recovery<br><br>
            If you are unsure whether this app is safe for you, please consult a licensed healthcare professional before use. <b>Stop using the app immediately</b> and seek medical attention if you feel pain, dizziness, or discomfort.<br><br>
            <b>3. User Responsibilities</b><br>
            By using Nutriority, you agree to:<br>
            • Provide honest and accurate information about yourself<br>
            • Use the app only for personal health and fitness purposes<br>
            • Not share your account with others<br>
            • Not misuse or attempt to damage the app or its data<br><br>
            <b>4. Privacy</b><br>
            Any personal information you provide (such as your name, age, and health data) will only be used to operate and improve Nutriority. Your data will be handled securely by <b>Xfactor Fitness Gym</b>.<br><br>
            <b>5. Limitation of Liability</b><br>
            Nutriority and <b>Xfactor Fitness Gym</b> are not liable for any injury, health issue, or damages that may result from using the app. You use the app at your own risk.<br><br>
            <b>6. Changes to These Terms</b><br>
            We may update these Terms from time to time. Continued use of the app after any changes means you accept the updated Terms.<br><br>
            <b>7. Contact</b><br>
            For questions or concerns, please reach out to the <b>Xfactor Fitness Gym</b> team through our official contact channels.<br><br>
            By using Nutriority, you confirm that you have read and agree to these Terms and Conditions.
        """.trimIndent()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Terms and Conditions")
            .setMessage(HtmlCompat.fromHtml(termsAndServicesText, HtmlCompat.FROM_HTML_MODE_COMPACT))
            .setPositiveButton("OK", null)
            .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
    }
}
