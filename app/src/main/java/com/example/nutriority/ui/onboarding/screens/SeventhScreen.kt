package com.example.nutriority.ui.onboarding.screens

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.nutriority.R
import com.example.nutriority.data.UserViewModel
import com.example.nutriority.data.model.User
import com.example.nutriority.databinding.FragmentSeventhScreenBinding
import com.example.nutriority.planner.PlannerService
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
    private var pulseAnimator: AnimatorSet? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSeventhScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Ensure initial state
        binding.bmiCard.alpha = 0f
        binding.bmiCard.visibility = View.INVISIBLE
        
        startDoublePulseAnimation()

        val parentVp = parentFragment?.view?.findViewById<ViewPager2>(R.id.viewPager)
        if (parentVp?.currentItem == 8) {
            startRecapIfNeeded()
        }

        parentFragmentManager.setFragmentResultListener("pageSelected", this) { _, bundle ->
            val position = bundle.getInt("position", -1)
            if (position == 8) startRecapIfNeeded()
        }
    }

    private fun startDoublePulseAnimation() {
        // Inner Pulse
        val innerScaleX = ObjectAnimator.ofFloat(binding.pulseInner, "scaleX", 1f, 1.4f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val innerScaleY = ObjectAnimator.ofFloat(binding.pulseInner, "scaleY", 1f, 1.4f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val innerAlpha = ObjectAnimator.ofFloat(binding.pulseInner, "alpha", 0.2f, 0.05f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }

        // Outer Pulse
        val outerScaleX = ObjectAnimator.ofFloat(binding.pulseOuter, "scaleX", 1f, 1.6f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            startDelay = 200
        }
        val outerScaleY = ObjectAnimator.ofFloat(binding.pulseOuter, "scaleY", 1f, 1.6f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            startDelay = 200
        }
        val outerAlpha = ObjectAnimator.ofFloat(binding.pulseOuter, "alpha", 0.1f, 0.02f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            startDelay = 200
        }

        pulseAnimator = AnimatorSet().apply {
            playTogether(innerScaleX, innerScaleY, innerAlpha, outerScaleX, outerScaleY, outerAlpha)
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun runRecapAnimation() {
        lifecycleScope.launch {
            val user = userViewModel.user.value ?: return@launch
            val (initial, details) = buildRecap(user)

            // 1. Animated details recap
            for ((i, text) in initial.withIndex()) {
                animateRecapText(text)
                delay(1500)

                if (i == 2) showBmiWithAnimation(user)
            }

            for (text in details) {
                animateRecapText(text)
                delay(if (text.contains("Finalizing")) 3500 else 1500)
            }

            // 2. Final Plan Generation and Navigation
            try {
                val generatedPersonalizedPlan = plannerService.generatePlanForUser(user)
                val workoutPlanJson = Gson().toJson(generatedPersonalizedPlan.workoutPlan)
                val saveSuccess = userViewModel.savePersonalizedPlanAndAwait(workoutPlanJson)

                if (saveSuccess) {
                    finishOnboarding()
                    findNavController().navigate(R.id.action_viewPagerFragment_to_homeFragment)
                } else {
                    Log.e("OnboardingError", "Failed to save the personalized workout plan.")
                }
            } catch (e: Exception) {
                Log.e("OnboardingError", "An error occurred during plan generation.", e)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun showBmiWithAnimation(user: User) {
        val heightM = user.heightCm / 100.0
        if (heightM <= 0 || user.weightKg <= 0) return

        val bmi = (user.weightKg / heightM.pow(2)).toFloat()

        val (label, colorRes) = when {
            bmi < 18.5 -> "UNDERWEIGHT" to android.R.color.holo_blue_dark
            bmi < 25 -> "NORMAL WEIGHT" to R.color.green
            bmi < 30 -> "OVERWEIGHT" to android.R.color.holo_orange_dark
            else -> "OBESE" to android.R.color.holo_red_dark
        }

        val color = ContextCompat.getColor(requireContext(), colorRes)

        binding.bmiValueText.text = String.format("%.1f", bmi)
        binding.bmiCategoryText.text = label
        binding.bmiValueText.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
        binding.bmiCategoryText.setTextColor(color)

        // Smooth Reveal: Slide up + Fade in
        binding.bmiCard.visibility = View.VISIBLE
        binding.bmiCard.translationY = 200f
        binding.bmiCard.alpha = 0f
        
        binding.bmiCard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1200)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    private fun animateRecapText(newText: String) {
        val v = binding.userDataRecapText
        
        // Premium Text Animation: Slide Up + Fade Out, then Slide Up + Fade In
        val fadeOut = ObjectAnimator.ofFloat(v, "alpha", 1f, 0f)
        val slideOut = ObjectAnimator.ofFloat(v, "translationY", 0f, -60f)

        val outSet = AnimatorSet().apply {
            playTogether(fadeOut, slideOut)
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
        }

        outSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                v.text = newText
                v.translationY = 60f // Start from below for the next text
                
                val fadeIn = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f)
                val slideIn = ObjectAnimator.ofFloat(v, "translationY", 40f, 0f)
                
                AnimatorSet().apply {
                    playTogether(fadeIn, slideIn)
                    duration = 600
                    interpolator = DecelerateInterpolator()
                    start()
                }
            }
        })

        outSet.start()
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
            val exclusions = user.excludedIngredients.joinToString(", ")
            details.add("Exclusions: ${if (exclusions.length > 25) exclusions.take(22) + "..." else exclusions}")
        }

        details.add("Finalizing your plan...")

        return initial to details
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pulseAnimator?.cancel()
        _binding = null
    }
}
