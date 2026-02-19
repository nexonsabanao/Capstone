package com.example.nutriority.ui.workout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.nutriority.R
import com.example.nutriority.data.model.WorkoutSessionLog
import com.example.nutriority.databinding.FragmentWorkoutCompleteBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.profile.ProfileViewModel
import com.example.nutriority.ui.util.ImageUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.pow

@AndroidEntryPoint
class WorkoutCompleteFragment : Fragment() {

    private var _binding: FragmentWorkoutCompleteBinding? = null
    private val binding get() = _binding!!

    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private val viewModel: WorkoutDetailViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private var userHeight = 170.0
    private var isUpdatingWeight = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeSummary()
        setupWeightLogging()
        
        // Accurate real-time calendar
        profileViewModel.sessionLogs.observe(viewLifecycleOwner) { logs ->
            setupCalendar(logs)
        }
        
        binding.btnFinish.setOnClickListener {
            finishAndGoHome()
        }

        // Prevent back press from leaving this screen without cleaning up
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndGoHome()
            }
        })
    }

    private fun finishAndGoHome() {
        viewModel.stopWorkout(save = true)
        navigationViewModel.resetToHome()
    }

    override fun onResume() {
        super.onResume()
        // Trigger celebration on resume to ensure it shows up after transitions
        viewLifecycleOwner.lifecycleScope.launch {
            // Wait for fragment transition to complete
            delay(600)
            startCelebration()
        }
    }

    private fun observeSummary() {
        viewModel.latestSessionLog.observe(viewLifecycleOwner) { log: WorkoutSessionLog? ->
            log?.let {
                binding.tvStatExercises.text = it.exercisesDone.toString()
                binding.tvStatTime.text = viewModel.formatElapsedTime(it.durationSeconds)
                binding.tvWorkoutSummary.text = it.workoutName
                binding.tvStatCalories.text = it.caloriesBurned.toString()

                // Set dynamic banner image using the utility
                val resId = ImageUtil.getWorkoutImageResource("", it.workoutName, it.difficulty)
                binding.ivWorkoutBanner.setImageResource(resId)
            }
        }
    }

    private fun startCelebration() {
        if (_binding == null) return
        
        // Intensified celebration: more duration, bigger particles
        val party = Party(
            speed = 15f,
            maxSpeed = 45f,
            damping = 0.9f,
            spread = 360,
            size = listOf(Size.SMALL, Size.MEDIUM, Size.LARGE, Size(12)), 
            colors = listOf(0x00A78B, 0xFFD700, 0xFF5252, 0xFFFFFF, 0x00A4B9),
            position = Position.Relative(0.5, 0.2), 
            emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(100) 
        )
        
        binding.konfettiView.start(party)
    }

    private fun setupCalendar(logs: List<WorkoutSessionLog>) {
        val container = binding.calendarContainer
        container.removeAllViews()

        val calendar = Calendar.getInstance()
        val todayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val completedDays = logs.filter { 
            val logCal = Calendar.getInstance().apply { timeInMillis = it.date }
            logCal.get(Calendar.YEAR) == currentYear
        }.map { 
            val logCal = Calendar.getInstance().apply { timeInMillis = it.date }
            logCal.get(Calendar.DAY_OF_YEAR)
        }.toSet()

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val dayInitials = arrayOf("S", "M", "T", "W", "T", "F", "S")

        for (i in 0..6) {
            val dayLayout = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val isCompleted = completedDays.contains(dayOfYear)
            val isToday = dayOfYear == todayOfYear

            val dayIndicator = TextView(requireContext()).apply {
                val size = (36 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size)
                gravity = Gravity.CENTER
                textSize = 14f
                
                if (isCompleted) {
                    background = ContextCompat.getDrawable(context, R.drawable.ic_check_circle)
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.green)
                    text = ""
                } else if (isToday) {
                    background = ContextCompat.getDrawable(context, R.drawable.bg_set_number_active)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    text = calendar.get(Calendar.DAY_OF_MONTH).toString()
                    setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    background = ContextCompat.getDrawable(context, R.drawable.bg_set_number_inactive)
                    setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                    text = calendar.get(Calendar.DAY_OF_MONTH).toString()
                }
            }

            val initialText = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (4 * resources.displayMetrics.density).toInt()
                }
                text = dayInitials[i]
                textSize = 10f
                if (isToday) {
                    setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                } else if (isCompleted) {
                    setTextColor(ContextCompat.getColor(context, R.color.green))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                }
            }

            dayLayout.addView(dayIndicator)
            dayLayout.addView(initialText)
            container.addView(dayLayout)
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun setupWeightLogging() {
        profileViewModel.getUser.observe(viewLifecycleOwner) { user ->
            if (isUpdatingWeight) return@observe
            user?.let {
                userHeight = it.heightCm
                val isKg = binding.weightToggleGroup.checkedButtonId == R.id.btnKg
                val weightToDisplay = if (isKg) it.weightKg else it.weightKg * 2.20462
                val weightStr = String.format("%.1f", weightToDisplay)
                
                if (binding.etWeight.text.toString() != weightStr) {
                    binding.etWeight.setText(weightStr)
                }
                updateBmi(it.weightKg)
            }
        }

        binding.weightToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isUpdatingWeight = true
                val currentInput = binding.etWeight.text.toString().toDoubleOrNull() ?: 0.0
                if (checkedId == R.id.btnLb) {
                    // Switch to LB: multiply by 2.20462
                    binding.etWeight.setText(String.format("%.1f", currentInput * 2.20462))
                    binding.tvWeightUnitLabel.text = "lb"
                } else {
                    // Switch back to KG: divide by 2.20462
                    binding.etWeight.setText(String.format("%.1f", currentInput / 2.20462))
                    binding.tvWeightUnitLabel.text = "kg"
                }
                binding.etWeight.postDelayed({ isUpdatingWeight = false }, 100)
            }
        }

        binding.etWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingWeight) return
                val inputWeight = s.toString().toDoubleOrNull() ?: return
                
                val isKg = binding.weightToggleGroup.checkedButtonId == R.id.btnKg
                val weightInKg = if (isKg) inputWeight else inputWeight / 2.20462
                
                updateBmi(weightInKg)
                profileViewModel.updateWeight(weightInKg)
            }
        })
    }

    private fun updateBmi(weightKg: Double) {
        if (!isAdded || userHeight <= 0) return
        val bmi = weightKg / (userHeight / 100.0).pow(2)
        binding.tvBmiValue.text = String.format("%.1f", bmi)
        
        val (status, color) = when {
            bmi < 18.5 -> "Underweight" to android.graphics.Color.parseColor("#4A90E2")
            bmi < 25 -> "Normal" to ContextCompat.getColor(requireContext(), R.color.green)
            bmi < 30 -> "Overweight" to android.graphics.Color.parseColor("#F5A623")
            else -> "Obese" to android.graphics.Color.parseColor("#D0021B")
        }
        
        binding.tvBmiStatus.text = status
        binding.tvBmiStatus.setTextColor(color)

        val progress = ((bmi - 15) / (40 - 15)).coerceIn(0.0, 1.0).toFloat()
        val params = binding.tvBmiValue.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.horizontalBias = progress
        binding.tvBmiValue.layoutParams = params
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
