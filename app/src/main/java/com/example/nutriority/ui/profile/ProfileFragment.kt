package com.example.nutriority.ui.profile

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nutriority.R
import com.example.nutriority.data.model.WorkoutSessionLog
import com.example.nutriority.databinding.FragmentProfileBinding
import com.example.nutriority.ui.NavigationViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Find LOG button in calorie card by looking for the TextView with text "LOG"
        // or using the specific IDs if they exist. Since IDs might be duplicated in includes,
        // we use the binding if the include has an ID.
        
        // Calorie card LOG button
        binding.calorieCard.root.findViewById<TextView>(R.id.btn_log_weight)?.setOnClickListener {
            // This is actually in the weight card in your layout structure usually, 
            // but let's look at where "LOG" is defined. 
            // In item_preview_calorie_intake.xml, the "LOG" is next to "Macros"
        }
        
        // Macro section LOG button
        binding.calorieCard.root.findViewById<View>(R.id.macroCard)?.findViewById<TextView>(R.id.btn_log_weight)?.setOnClickListener {
            navigationViewModel.setTab(2)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.getUser.observe(viewLifecycleOwner) { user ->
                    user?.let {
                        val bmi = it.weightKg / (it.heightCm / 100.0).pow(2)
                        updateCalorieCard(it.weightKg, bmi)
                        updateWeightChartFromLogs(profileViewModel.sessionLogs.value ?: emptyList())
                        
                        binding.root.findViewById<TextView>(R.id.tv_current_weight)?.text = String.format("%.1f kg", it.weightKg)
                    }
                }

                profileViewModel.sessionLogs.observe(viewLifecycleOwner) { logs ->
                    updateActivityStats(logs)
                    setupCalendar(logs)
                    updateWeightChartFromLogs(logs)
                    updateStreak(logs)
                }
            }
        }
    }

    private fun updateActivityStats(logs: List<WorkoutSessionLog>) {
        val totalWorkouts = logs.size
        val totalCalories = logs.sumOf { it.caloriesBurned }
        val totalMinutes = logs.sumOf { it.durationSeconds } / 60

        binding.tvWorkoutsCount.text = totalWorkouts.toString()
        binding.tvKcalCount.text = totalCalories.toString()
        binding.tvMinutesCount.text = totalMinutes.toString()
    }

    private fun setupCalendar(logs: List<WorkoutSessionLog>) {
        val table = binding.root.findViewById<android.widget.TableLayout>(R.id.calendar_table) ?: return
        
        if (table.childCount > 1) {
            table.removeViews(1, table.childCount - 1)
        }

        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        
        val row = TableRow(requireContext())
        row.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
        row.setPadding(0, 16, 0, 0)

        val logDates = logs.map { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.toSet()

        for (i in 0..6) {
            val dateText = TextView(requireContext())
            val dayNum = calendar.get(Calendar.DAY_OF_MONTH)
            val dateKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
            
            dateText.text = dayNum.toString()
            dateText.gravity = Gravity.CENTER
            dateText.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            dateText.textSize = 14f
            
            when {
                logDates.contains(dateKey) -> {
                    dateText.setBackgroundResource(R.drawable.bg_circle_green)
                    dateText.setTextColor(Color.WHITE)
                    dateText.setTypeface(null, Typeface.BOLD)
                }
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                    dateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                    dateText.setTypeface(null, Typeface.BOLD)
                }
                calendar.after(today) -> {
                    dateText.setTextColor(Color.parseColor("#BDBDBD"))
                }
                else -> {
                    dateText.setTextColor(Color.parseColor("#212121"))
                }
            }

            val params = TableRow.LayoutParams(0, 100, 1f)
            dateText.layoutParams = params
            row.addView(dateText)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        table.addView(row)
    }

    private fun updateStreak(logs: List<WorkoutSessionLog>) {
        if (logs.isEmpty()) {
            setStreakText(0)
            return
        }

        val calendar = Calendar.getInstance()
        val todayStr = getDayKey(calendar)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = getDayKey(calendar)

        val logDates = logs.map { 
            val logCal = Calendar.getInstance().apply { timeInMillis = it.date }
            getDayKey(logCal)
        }.toSet()

        if (!logDates.contains(todayStr) && !logDates.contains(yesterdayStr)) {
            setStreakText(0)
            return
        }

        var streak = 0
        val checkCal = Calendar.getInstance()
        
        if (!logDates.contains(todayStr)) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (logDates.contains(getDayKey(checkCal))) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        setStreakText(streak)
    }

    private fun getDayKey(cal: Calendar) = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"

    private fun setStreakText(streak: Int) {
        // Use the ID I added to the layout directly
        binding.root.findViewById<TextView>(R.id.tvStreakCount)?.text = streak.toString()
    }

    private fun updateWeightChartFromLogs(logs: List<WorkoutSessionLog>) {
        val chart = binding.root.findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.line_chart) ?: return
        
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        
        val dateFormat = SimpleDateFormat("M/dd", Locale.getDefault())
        val labels = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        
        val lastSevenDaysKeys = mutableListOf<String>()
        for (i in 0..6) {
            labels.add(dateFormat.format(calendar.time))
            lastSevenDaysKeys.add(getDayKey(calendar))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(false)
            granularity = 1f
            textColor = Color.parseColor("#9E9E9E")
            valueFormatter = IndexAxisValueFormatter(labels)
        }
        
        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.parseColor("#F5F5F5")
            setDrawAxisLine(false)
            textColor = Color.parseColor("#9E9E9E")
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = value.toInt().toString()
            }
        }
        chart.axisRight.isEnabled = false

        val weightEntries = ArrayList<Entry>()
        val currentProfileWeight = profileViewModel.getUser.value?.weightKg ?: 0.0
        
        lastSevenDaysKeys.forEachIndexed { index, dateKey ->
            val logForDay = logs.find { 
                val logCal = Calendar.getInstance().apply { timeInMillis = it.date }
                getDayKey(logCal) == dateKey 
            }
            
            if (logForDay != null && logForDay.weightKg > 0) {
                weightEntries.add(Entry(index.toFloat(), logForDay.weightKg.toFloat()))
            } else if (index == 6) {
                weightEntries.add(Entry(index.toFloat(), currentProfileWeight.toFloat()))
            }
        }

        if (weightEntries.isNotEmpty()) {
            val dataSet = LineDataSet(weightEntries, "Weight").apply {
                color = ContextCompat.getColor(requireContext(), R.color.green)
                lineWidth = 3f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
                setDrawHighlightIndicators(false)
                setDrawCircles(true)
                circleColors = weightEntries.mapIndexed { index, _ -> 
                    if (index == weightEntries.size - 1) ContextCompat.getColor(requireContext(), R.color.green) 
                    else Color.TRANSPARENT 
                }
                circleRadius = 5f
                circleHoleRadius = 3f
                setCircleColor(ContextCompat.getColor(requireContext(), R.color.green))
                setDrawFilled(true)
                fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_gradient_fill)
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
            
            val weights = weightEntries.map { it.y }
            binding.root.findViewById<TextView>(R.id.tv_heaviest_weight)?.text = String.format("%.1f kg", weights.maxOrNull() ?: currentProfileWeight)
            binding.root.findViewById<TextView>(R.id.tv_lightest_weight)?.text = String.format("%.1f kg", weights.minOrNull() ?: currentProfileWeight)
        }
    }

    private fun updateCalorieCard(weight: Double, bmi: Double) {
        binding.root.findViewById<TextView>(R.id.tvBmiValue)?.text = String.format("%.1f", bmi)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
