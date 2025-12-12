package com.example.nutriority.ui.onboarding.screens

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.data.model.User
import com.example.nutriority.databinding.FragmentSeventhScreenBinding
import com.example.nutriority.planner.PlannerService
import com.example.nutriority.ui.BottomNavigationActivity
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.pow

@AndroidEntryPoint
class SeventhScreen : Fragment() {

    @Inject
    lateinit var plannerService: PlannerService

    private var _binding: FragmentSeventhScreenBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    private var recapAnimationStarted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSeventhScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bmiContainer.alpha = 0f

        val parentVp = parentFragment?.view?.findViewById<ViewPager2>(R.id.viewPager)
        if (parentVp?.currentItem == 8) {
            startRecapIfNeeded()
        }

        parentFragmentManager.setFragmentResultListener("pageSelected", this) { _, bundle ->
            val position = bundle.getInt("position", -1)
            if (position == 8) startRecapIfNeeded()
        }
    }

    private fun runRecapAnimation() {
        lifecycleScope.launch {
            val user = userViewModel.user.value ?: return@launch
            val (initial, details) = buildRecap(user)

            for ((i, text) in initial.withIndex()) {
                fadeText(text)
                delay(1000)

                if (i == 2) calculateBmi(user)
            }

            for (text in details) {
                fadeText(text)
                delay(if (text.contains("Finalizing")) 4000 else 1000)
            }

            try {
                val generatedPlan = plannerService.generatePlanForUser(user)
                val planJson = Gson().toJson(generatedPlan)
                val saveSuccess = userViewModel.savePersonalizedPlanAndAwait(planJson)

                if (saveSuccess) {
                    finishOnboarding()
                    val intent = Intent(requireActivity(), BottomNavigationActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    Log.e("OnboardingError", "Failed to save the personalized plan to the database.")
                    // Optionally, show an error message to the user
                }

            } catch (e: Exception) {
                Log.e("OnboardingError", "An error occurred during plan generation or saving.", e)
                // Optionally, show an error message to the user
            }
        }
    }

    private fun startRecapIfNeeded() {
        if (!recapAnimationStarted) {
            recapAnimationStarted = true
            runRecapAnimation()
        }
    }

    private fun finishOnboarding() {
        requireActivity()
            .getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("Finished", true)
            .apply()
    }

    private fun buildRecap(user: User): Pair<List<String>, List<String>> {
        val height = if (user.unitSystem == "IMPERIAL") {
            val totalIn = user.heightCm / 2.54
            val ft = floor(totalIn / 12).toInt()
            val inch = (totalIn % 12).toInt()
            "Height: ${ft}'${inch}\" (${user.heightCm.toInt()} cm)"
        } else {
            "Height: ${user.heightCm.toInt()} cm"
        }

        val weight = if (user.unitSystem == "IMPERIAL") {
            val lbs = (user.weightKg * 2.20462).toInt()
            "Weight: $lbs lbs (${user.weightKg.toInt()} kg)"
        } else {
            "Weight: ${user.weightKg.toInt()} kg"
        }

        val initial = listOf(
            "Gender: ${user.gender}",
            height,
            weight
        )

        val details = mutableListOf(
            "Activity: ${user.activityLevel}",
            "Diet: ${user.preferredDiet}",
            "Goal: ${user.goal}"
        )

        if (user.excludedIngredients.isNotEmpty()) {
            details.add("Exclusions: ${user.excludedIngredients.joinToString(", ")}")
        }

        details.add("Finalizing your plan...")

        return initial to details
    }

    private fun calculateBmi(user: User) {
        val heightM = user.heightCm / 100.0
        if (heightM <= 0 || user.weightKg <= 0) return

        val bmi = (user.weightKg / heightM.pow(2)).toFloat()

        val (label, colorRes) = when {
            bmi < 18.5 -> "Underweight" to android.R.color.holo_blue_dark
            bmi < 25 -> "Normal" to R.color.green
            bmi < 30 -> "Overweight" to android.R.color.holo_orange_dark
            else -> "Obese" to android.R.color.holo_red_dark
        }

        val color = ContextCompat.getColor(requireContext(), colorRes)

        binding.bmiValueText.text = String.format("%.1f", bmi)
        binding.bmiCategoryText.text = label
        binding.bmiValueText.setTextColor(color)
        binding.bmiCategoryText.setTextColor(color)
        binding.bmiContainer.animate().alpha(1f).setDuration(400).start()
    }

    private fun fadeText(newText: String) {
        val v = binding.userDataRecapText
        val fadeOut = ObjectAnimator.ofFloat(v, "alpha", 1f, 0f).apply {
            duration = 250
            interpolator = AccelerateInterpolator()
        }

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                v.text = newText
                ObjectAnimator.ofFloat(v, "alpha", 0f, 1f).apply {
                    duration = 250
                }.start()
            }
        })

        fadeOut.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
