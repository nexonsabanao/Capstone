package com.example.nutriority.ui.profile

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.data.model.User
import com.example.nutriority.data.model.WorkoutSessionLog
import com.example.nutriority.data.model.DailyMealLog
import com.example.nutriority.databinding.FragmentProfileBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.custom.WeightMarkerView
import com.example.nutriority.ui.util.AgeUtil
import com.example.nutriority.ui.util.BaseBindingFragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ProfileFragment : BaseBindingFragment<FragmentProfileBinding>(FragmentProfileBinding::inflate) {

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    
    private var isFullMonthView = false
    private val currentDisplayDate = Calendar.getInstance()

    private val loggedFoodAdapter by lazy {
        LoggedFoodAdapter { log ->
            profileViewModel.deleteMealLog(log)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvLoggedFood.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = loggedFoodAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.btnSettings.setOnClickListener {
            navigationViewModel.navigateToEditProfile()
        }

        binding.weightCard.root.findViewById<View>(R.id.btn_log_weight)?.setOnClickListener {
            showWeightLogBottomSheet()
        }

        binding.calorieCard.root.findViewById<View>(R.id.btnLogFood)?.setOnClickListener {
            if (isProfileComplete()) {
                navigationViewModel.navigateToLogManual()
            }
        }
        
        binding.historyCard.btnShowRecords.setOnClickListener {
            isFullMonthView = !isFullMonthView
            binding.historyCard.btnShowRecords.text = if (isFullMonthView) "WEEKLY" else "CALENDAR"
            binding.historyCard.layoutNavMonth.isVisible = isFullMonthView
            
            if (!isFullMonthView) {
                currentDisplayDate.time = Date() 
                binding.historyCard.textHistoryTitle.text = "History"
            }
            
            profileViewModel.sessionLogs.value?.let { setupCalendar(it) }
        }

        binding.historyCard.btnPrevMonth.setOnClickListener {
            currentDisplayDate.add(Calendar.MONTH, -1)
            profileViewModel.sessionLogs.value?.let { setupCalendar(it) }
        }

        binding.historyCard.btnNextMonth.setOnClickListener {
            val nextMonth = currentDisplayDate.clone() as Calendar
            nextMonth.add(Calendar.MONTH, 1)
            
            if (nextMonth.before(Calendar.getInstance()) || 
                (nextMonth.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) && 
                 nextMonth.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR))) {
                currentDisplayDate.add(Calendar.MONTH, 1)
                profileViewModel.sessionLogs.value?.let { setupCalendar(it) }
            }
        }
    }

    private fun isProfileComplete(): Boolean {
        val user = profileViewModel.uiState.value.user
        if (user == null) {
            Toast.makeText(requireContext(), "Loading user profile...", Toast.LENGTH_SHORT).show()
            return false
        }

        val missingFields = mutableListOf<String>()
        if (user.birthDate == null) missingFields.add("Birth Date")
        if (user.heightCm <= 0) missingFields.add("Height")
        if (user.weightKg <= 0) missingFields.add("Weight")
        if (user.gender.isBlank()) missingFields.add("Gender")
        if (user.activityLevel.isBlank()) missingFields.add("Activity Level")
        if (user.goal.isBlank()) missingFields.add("Fitness Goal")

        return if (missingFields.isNotEmpty()) {
            showProfileIncompleteDialog(missingFields)
            false
        } else {
            true
        }
    }

    private fun showProfileIncompleteDialog(missingFields: List<String>) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile_incomplete, null)
        
        val tvMissing = dialogView.findViewById<TextView>(R.id.tvMissingFields)
        val btnGoToProfile = dialogView.findViewById<MaterialButton>(R.id.btnGoToProfile)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        tvMissing.text = "Missing: ${missingFields.joinToString(", ")}"

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnGoToProfile.setOnClickListener {
            dialog.dismiss()
            navigationViewModel.navigateToEditProfile()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showWeightLogBottomSheet() {
        val currentWeight = profileViewModel.uiState.value.user?.weightKg ?: 60.0
        val bottomSheet = WeightLogBottomSheetFragment(currentWeight) { weight, date ->
            profileViewModel.logWeight(weight, date)
        }
        bottomSheet.show(childFragmentManager, "WeightLogBottomSheet")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.uiState.collectLatest { state ->
                    if (state.isInitialLoading) return@collectLatest

                    loggedFoodAdapter.submitList(state.todayMealLogs)
                    val hasLogs = state.todayMealLogs.isNotEmpty()
                    binding.tvFoodTitle.isVisible = hasLogs
                    binding.rvLoggedFood.isVisible = hasLogs

                    state.user?.let { user ->
                        binding.weightCard.tvCurrentWeight.text = String.format("%.1f kg", user.weightKg)
                        updateCalorieCard(user, state.todayMealLogs)
                        updateWeightChartFromLogs(state.sessionLogs, user.weightKg)
                    }

                    updateActivityStats(state.sessionLogs)
                    setupCalendar(state.sessionLogs)
                    updateStreak(state.sessionLogs)
                }
            }
        }
    }

    private fun updateActivityStats(logs: List<WorkoutSessionLog>) {
        val workoutLogs = logs.filter { it.workoutId != 0 }
        
        val today = Calendar.getInstance()
        val todayLogs = workoutLogs.filter {
            val logCal = Calendar.getInstance().apply { timeInMillis = it.date }
            logCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            logCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        }

        val totalWorkouts = todayLogs.size
        val totalCalories = todayLogs.sumOf { it.caloriesBurned }
        val totalMinutes = todayLogs.sumOf { it.durationSeconds } / 60

        binding.tvWorkoutsCount.text = totalWorkouts.toString()
        binding.tvKcalCount.text = totalCalories.toString()
        binding.tvMinutesCount.text = totalMinutes.toString()
    }

    private fun setupCalendar(logs: List<WorkoutSessionLog>) {
        val table = binding.historyCard.calendarTable
        if (table.childCount > 1) table.removeViews(1, table.childCount - 1)

        val calendar = currentDisplayDate.clone() as Calendar
        val today = Calendar.getInstance()
        
        val logDates = logs.filter { it.workoutId != 0 }
            .map { getDayKey(Calendar.getInstance().apply { timeInMillis = it.date }) }
            .toSet()

        if (isFullMonthView) {
            val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            binding.historyCard.textHistoryTitle.text = monthYearFormat.format(calendar.time)

            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val currentMonth = calendar.get(Calendar.MONTH)
            
            while (calendar.get(Calendar.DAY_OF_WEEK) != calendar.firstDayOfWeek) {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            }

            for (w in 0..5) { 
                val row = TableRow(requireContext())
                row.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
                row.setPadding(0, 8, 0, 8)
                
                var hasDaysInMonth = false
                for (d in 0..6) {
                    if (calendar.get(Calendar.MONTH) == currentMonth) hasDaysInMonth = true
                    
                    val dateText = createDateTextView(calendar, logDates, today, calendar.get(Calendar.MONTH) != currentMonth)
                    row.addView(dateText)
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                table.addView(row)
                if (!hasDaysInMonth && w > 3) break 
            }
            
            val nextMonth = currentDisplayDate.clone() as Calendar
            nextMonth.add(Calendar.MONTH, 1)
            val isFuture = nextMonth.after(Calendar.getInstance()) && 
                          !(nextMonth.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) && 
                            nextMonth.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR))
            
            binding.historyCard.btnNextMonth.alpha = if (isFuture) 0.3f else 1.0f
        } else {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            val row = TableRow(requireContext())
            row.setPadding(0, 8, 0, 8)
            for (i in 0..6) {
                row.addView(createDateTextView(calendar, logDates, today, false))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            table.addView(row)
        }
    }

    private fun createDateTextView(cal: Calendar, logDates: Set<String>, today: Calendar, isFaded: Boolean): TextView {
        val tv = TextView(requireContext())
        tv.text = cal.get(Calendar.DAY_OF_MONTH).toString()
        tv.gravity = Gravity.CENTER
        tv.textSize = 14f
        tv.setTypeface(null, Typeface.BOLD)
        
        val layoutParams = TableRow.LayoutParams(0, 100, 1f)
        tv.layoutParams = layoutParams

        val dayKey = getDayKey(cal)
        val isToday = dayKey == getDayKey(today)
        val hasLog = logDates.contains(dayKey)

        when {
            hasLog -> {
                tv.setBackgroundResource(R.drawable.bg_calendar_active)
                tv.setTextColor(Color.parseColor("#757575"))
            }
            isToday -> {
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            }
            isFaded -> {
                tv.setTextColor(Color.LTGRAY)
            }
            else -> {
                tv.setTextColor(Color.parseColor("#212121"))
            }
        }
        return tv
    }

    private fun updateStreak(logs: List<WorkoutSessionLog>) {
        val logDates = logs.filter { it.workoutId != 0 }
            .map { getDayKey(Calendar.getInstance().apply { timeInMillis = it.date }) }
            .toSet()

        var streak = 0
        val checkCal = Calendar.getInstance()
        val todayKey = getDayKey(checkCal)
        
        val loggedToday = logDates.contains(todayKey)
        if (!loggedToday) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (logDates.contains(getDayKey(checkCal))) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        binding.historyCard.tvStreakCount.text = streak.toString()
        binding.historyCard.tvStreakLabel.text = if (streak <= 1) "day" else "days"
        
        if (streak == 0) {
            binding.historyCard.tvStreakCount.setTextColor(Color.parseColor("#E74C3C"))
        } else {
            binding.historyCard.tvStreakCount.setTextColor(Color.parseColor("#212121"))
        }
    }

    private fun getDayKey(cal: Calendar) = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"

    private fun updateWeightChartFromLogs(logs: List<WorkoutSessionLog>, currentWeight: Double) {
        val chart = binding.weightCard.lineChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        
        val marker = WeightMarkerView(requireContext(), R.layout.layout_weight_marker)
        marker.chartView = chart
        chart.marker = marker

        val groupedLogs = logs.filter { it.weightKg > 0 }
            .groupBy { getDayKey(Calendar.getInstance().apply { timeInMillis = it.date }) }
            .map { it.value.maxByOrNull { log -> log.date }!! }
            .sortedBy { it.date }

        val allWeights = groupedLogs.map { it.weightKg }.toMutableList()
        if (currentWeight > 0) allWeights.add(currentWeight)
        
        if (allWeights.isNotEmpty()) {
            binding.weightCard.tvHeaviestWeight.text = String.format("%.1f", allWeights.maxOrNull() ?: 0.0)
            binding.weightCard.tvLightestWeight.text = String.format("%.1f", allWeights.minOrNull() ?: 0.0)
        }

        chart.xAxis.apply {
            setDrawGridLines(false)
            setDrawAxisLine(false)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.LTGRAY
            granularity = 1f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val entryLogs = groupedLogs
                    if (value.toInt() < 0 || value.toInt() >= entryLogs.size) return ""
                    val cal = Calendar.getInstance().apply { timeInMillis = entryLogs[value.toInt()].date }
                    return String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
                }
            }
        }
        
        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.parseColor("#F5F5F5")
            setDrawAxisLine(false)
            textColor = Color.LTGRAY
        }
        chart.axisRight.isEnabled = false

        val weightEntries = ArrayList<com.github.mikephil.charting.data.Entry>()
        groupedLogs.takeLast(7).forEachIndexed { i, log -> 
            weightEntries.add(com.github.mikephil.charting.data.Entry(i.toFloat(), log.weightKg.toFloat(), log.date)) 
        }

        if (weightEntries.isNotEmpty()) {
            val dataSet = LineDataSet(weightEntries, "Weight").apply {
                color = ContextCompat.getColor(requireContext(), R.color.green)
                lineWidth = 3f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_gradient_fill)
                setDrawCircles(true)
                setCircleColor(ContextCompat.getColor(requireContext(), R.color.green))
                circleRadius = 6f 
                setDrawCircleHole(true)
                circleHoleColor = Color.WHITE
                circleHoleRadius = 3f
                setDrawValues(false)
                
                setDrawHighlightIndicators(true)
                setDrawVerticalHighlightIndicator(true)
                setDrawHorizontalHighlightIndicator(false)
                highLightColor = ContextCompat.getColor(requireContext(), R.color.green)
                highlightLineWidth = 1f
            }
            
            chart.data = LineData(dataSet)
            chart.notifyDataSetChanged()
            chart.invalidate()
        } else {
            chart.clear()
        }
    }

    private fun updateCalorieCard(user: com.example.nutriority.data.model.User, logs: List<DailyMealLog>) {
        val totalLogged = logs.sumOf { it.calories }
        val goalCalories = com.example.nutriority.planner.NutritionCalculator.calculateTdeeDailyCalories(
            user.weightKg, user.heightCm, AgeUtil.calculateAge(user.birthDate), user.gender, user.activityLevel, user.goal
        )
        
        val left = (goalCalories - totalLogged).coerceAtLeast(0)
        val percentage = ((totalLogged.toFloat() / goalCalories) * 100).toInt().coerceIn(0, 100)

        val calCard = binding.calorieCard
        calCard.caloriesLeft.text = left.toString()
        calCard.caloriesPercentage.text = "$percentage%"
        calCard.circleCalories.progress = percentage.toFloat()
        
        val tvTotalKcal = calCard.root.findViewById<TextView>(R.id.tv_total_kcal)
        tvTotalKcal?.text = "$totalLogged kcal"

        val totalP = logs.sumOf { it.protein }
        val totalC = logs.sumOf { it.carbs }
        val totalF = logs.sumOf { it.fats }

        val targetP = (goalCalories * 0.15 / 4).toInt()
        val targetC = (goalCalories * 0.50 / 4).toInt()
        val targetF = (goalCalories * 0.35 / 9).toInt()

        // Set different colors for macro progress bars
        val macroLayout = calCard.macroProtein.root.parent as LinearLayout
        
        updateMacroItem(calCard.macroProtein.root, "PROTEIN", totalP, targetP, R.drawable.progress_bar_protein)
        updateMacroItem(calCard.macroCarbs.root, "CARBS", totalC, targetC, R.drawable.progress_bar_carbs)
        updateMacroItem(calCard.macroFats.root, "FATS", totalF, targetF, R.drawable.progress_bar_fats)
    }

    private fun updateMacroItem(container: View, label: String, current: Int, target: Int, drawableRes: Int) {
        val labelTv = container.findViewById<TextView>(R.id.macro_label)
        val valueTv = container.findViewById<TextView>(R.id.macro_value)
        val progress = container.findViewById<ProgressBar>(R.id.progressBar)

        labelTv?.text = label
        valueTv?.text = "$current/${target}g"
        progress?.max = target
        progress?.progress = current
        progress?.progressDrawable = ContextCompat.getDrawable(requireContext(), drawableRes)
    }
}
