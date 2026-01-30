package com.example.nutriority.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.MainActivity
import com.example.nutriority.R
import com.example.nutriority.data.model.WorkoutSessionLog
import com.example.nutriority.data.model.DailyMealLog
import com.example.nutriority.databinding.FragmentProfileBinding
import com.example.nutriority.ui.NavigationViewModel
import com.example.nutriority.ui.custom.WeightMarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    
    private var isFullMonthView = false
    private val currentDisplayDate = Calendar.getInstance()

    private val loggedFoodAdapter by lazy {
        LoggedFoodAdapter { log ->
            profileViewModel.deleteMealLog(log)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
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
        }
    }

    private fun setupClickListeners() {
        binding.weightCard.root.findViewById<View>(R.id.btn_log_weight)?.setOnClickListener {
            showWeightLogBottomSheet()
        }

        binding.headerContainer.setOnClickListener {
            showAccountOptionsDialog()
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

    private fun showWeightLogBottomSheet() {
        val currentWeight = profileViewModel.getUser.value?.weightKg ?: 60.0
        val bottomSheet = WeightLogBottomSheetFragment(currentWeight) { weight, date ->
            profileViewModel.logWeight(weight, date)
        }
        bottomSheet.show(childFragmentManager, "WeightLogBottomSheet")
    }

    private fun showAccountOptionsDialog() {
        val options = arrayOf("Logout", "Delete Account")
        AlertDialog.Builder(requireContext())
            .setTitle("Account Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> logout()
                    1 -> confirmDeleteAccount()
                }
            }
            .show()
    }

    private fun logout() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.clearAllLocalData()
            FirebaseAuth.getInstance().signOut()
            restartApp()
        }
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permanently Delete Account?")
            .setMessage("This will erase ALL your progress from the cloud and this phone. This action cannot be undone.")
            .setPositiveButton("DELETE EVERYTHING") { _, _ ->
                performFullDataWipe()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun performFullDataWipe() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val db = FirebaseFirestore.getInstance()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                db.collection("users").document(uid).delete()
                profileViewModel.clearAllLocalData()
                user.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Account Wiped Successfully", Toast.LENGTH_SHORT).show()
                        restartApp()
                    } else {
                        Toast.makeText(requireContext(), "Error: Re-login required to delete account.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Reset failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.getUser.observe(viewLifecycleOwner) { user ->
                    user?.let {
                        val bmi = it.weightKg / (it.heightCm / 100.0).pow(2)
                        updateWeightChartFromLogs(profileViewModel.sessionLogs.value ?: emptyList())
                        binding.weightCard.tvCurrentWeight.text = String.format("%.1f kg", it.weightKg)
                        profileViewModel.todayMealLogs.value?.let { logs -> updateCalorieCard(it, logs) }
                    }
                }

                profileViewModel.sessionLogs.observe(viewLifecycleOwner) { logs ->
                    updateActivityStats(logs)
                    setupCalendar(logs)
                    updateWeightChartFromLogs(logs)
                    updateStreak(logs)
                }

                profileViewModel.todayMealLogs.observe(viewLifecycleOwner) { logs ->
                    loggedFoodAdapter.submitList(logs)
                    profileViewModel.getUser.value?.let { user ->
                        updateCalorieCard(user, logs)
                    }
                }
            }
        }
    }

    private fun updateActivityStats(logs: List<WorkoutSessionLog>) {
        val totalWorkouts = logs.filter { it.workoutId != 0 }.size
        val totalCalories = logs.sumOf { it.caloriesBurned }
        val totalMinutes = logs.sumOf { it.durationSeconds } / 60

        binding.tvWorkoutsCount.text = totalWorkouts.toString()
        binding.tvKcalCount.text = totalCalories.toString()
        binding.tvMinutesCount.text = totalMinutes.toString()
    }

    private fun setupCalendar(logs: List<WorkoutSessionLog>) {
        val table = binding.historyCard.calendarTable
        if (table.childCount > 1) table.removeViews(1, table.childCount - 1)

        val calendar = currentDisplayDate.clone() as Calendar
        val today = Calendar.getInstance()
        val logDates = logs.filter { it.workoutId != 0 }.map { getDayKey(Calendar.getInstance().apply { timeInMillis = it.date }) }.toSet()

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
                tv.setTextColor(Color.WHITE)
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
        val workoutLogs = logs.filter { it.workoutId != 0 }
        if (workoutLogs.isEmpty()) {
            binding.historyCard.tvStreakCount.text = "0"
            return
        }
        val logDates = workoutLogs.map { getDayKey(Calendar.getInstance().apply { timeInMillis = it.date }) }.toSet()
        var streak = 0
        val checkCal = Calendar.getInstance()
        while (logDates.contains(getDayKey(checkCal))) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }
        binding.historyCard.tvStreakCount.text = streak.toString()
    }

    private fun getDayKey(cal: Calendar) = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"

    private fun updateWeightChartFromLogs(logs: List<WorkoutSessionLog>) {
        val chart = binding.weightCard.lineChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        
        val marker = WeightMarkerView(requireContext(), R.layout.layout_weight_marker)
        marker.chartView = chart
        chart.marker = marker

        chart.xAxis.apply {
            setDrawGridLines(false)
            setDrawAxisLine(false)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.LTGRAY
            granularity = 1f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val entryLogs = logs.filter { it.weightKg > 0 }.sortedBy { it.date }
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

        val weightLogs = logs.filter { it.weightKg > 0 }.sortedBy { it.date }
        val weightEntries = ArrayList<com.github.mikephil.charting.data.Entry>()
        
        weightLogs.takeLast(7).forEachIndexed { i, log -> 
            weightEntries.add(com.github.mikephil.charting.data.Entry(i.toFloat(), log.weightKg.toFloat(), log.date)) 
        }

        if (weightEntries.isNotEmpty()) {
            val maxWeight = weightLogs.maxOf { it.weightKg }
            val minWeight = weightLogs.minOf { it.weightKg }
            binding.weightCard.tvHeaviestWeight.text = String.format("%.0f", maxWeight)
            binding.weightCard.tvLightestWeight.text = String.format("%.0f", minWeight)

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
            chart.invalidate()
        }
    }

    private fun updateCalorieCard(user: com.example.nutriority.data.model.User, logs: List<DailyMealLog>) {
        val totalLogged = logs.sumOf { it.calories }
        val goalCalories = com.example.nutriority.planner.NutritionCalculator.calculateTdeeDailyCalories(
            user.weightKg, user.heightCm, user.age ?: 30, user.gender, user.activityLevel, user.goal
        )
        
        val left = (goalCalories - totalLogged).coerceAtLeast(0)
        val percentage = ((totalLogged.toFloat() / goalCalories) * 100).toInt().coerceIn(0, 100)

        val calCard = binding.calorieCard
        calCard.caloriesLeft.text = left.toString()
        calCard.caloriesPercentage.text = "$percentage%"
        calCard.circleCalories.progress = percentage.toFloat()
        
        calCard.root.findViewById<TextView>(R.id.foodHeader).parent.run {
            if (this is ViewGroup) {
                val kcalText = this.getChildAt(1) as? TextView
                kcalText?.text = "$totalLogged kcal"
            }
        }

        val totalP = logs.sumOf { it.protein }
        val totalC = logs.sumOf { it.carbs }
        val totalF = logs.sumOf { it.fats }

        val targetP = (goalCalories * 0.15 / 4).toInt()
        val targetC = (goalCalories * 0.50 / 4).toInt()
        val targetF = (goalCalories * 0.35 / 9).toInt()

        val macroListContainer = calCard.macroCard.getChildAt(0) as ViewGroup
        val macroLayout = macroListContainer.getChildAt(2) as LinearLayout
        
        updateMacroItem(macroLayout.getChildAt(0) as ViewGroup, "PROTEIN", totalP, targetP)
        updateMacroItem(macroLayout.getChildAt(1) as ViewGroup, "CARBS", totalC, targetC)
        updateMacroItem(macroLayout.getChildAt(2) as ViewGroup, "FATS", totalF, targetF)
    }

    private fun updateMacroItem(container: ViewGroup, label: String, current: Int, target: Int) {
        val labelTv = container.findViewById<TextView>(R.id.macro_label)
        val valueTv = container.findViewById<TextView>(R.id.macro_value)
        val progress = container.findViewById<ProgressBar>(R.id.progressBar)

        labelTv?.text = label
        valueTv?.text = "$current/${target}g"
        progress?.max = target
        progress?.progress = current
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
