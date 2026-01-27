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
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nutriority.MainActivity
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
        binding.calorieCard.root.findViewById<TextView>(R.id.btn_log_weight)?.setOnClickListener {
            navigationViewModel.setTab(2)
        }

        binding.headerContainer.setOnClickListener {
            showAccountOptionsDialog()
        }
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
            // CRITICAL: Wipe local data before logging out to prevent account leaking
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
                // 1. Delete Cloud Data
                db.collection("users").document(uid).delete()
                
                // 2. Wipe Local Phone Data (Room)
                profileViewModel.clearAllLocalData()

                // 3. Delete Login Credentials
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
                        updateCalorieCard(it.weightKg, bmi)
                        updateWeightChartFromLogs(profileViewModel.sessionLogs.value ?: emptyList())
                        binding.weightCard.tvCurrentWeight.text = String.format("%.1f kg", it.weightKg)
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
        val table = binding.historyCard.calendarTable
        if (table.childCount > 1) table.removeViews(1, table.childCount - 1)

        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        
        val row = TableRow(requireContext())
        val logDates = logs.map { getDayKey(Calendar.getInstance().apply { timeInMillis = it.date }) }.toSet()

        for (i in 0..6) {
            val dateText = TextView(requireContext())
            dateText.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
            dateText.gravity = Gravity.CENTER
            dateText.textSize = 14f
            
            if (logDates.contains(getDayKey(calendar))) {
                dateText.setBackgroundResource(R.drawable.ic_check_circle)
                dateText.setTextColor(Color.WHITE)
            } else if (getDayKey(calendar) == getDayKey(today)) {
                dateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            }

            dateText.layoutParams = TableRow.LayoutParams(0, 100, 1f)
            row.addView(dateText)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        table.addView(row)
    }

    private fun updateStreak(logs: List<WorkoutSessionLog>) {
        if (logs.isEmpty()) {
            binding.historyCard.tvStreakCount.text = "0"
            return
        }
        val logDates = logs.map { getDayKey(Calendar.getInstance().apply { timeInMillis = it.date }) }.toSet()
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
        val weightEntries = ArrayList<Entry>()
        logs.takeLast(7).forEachIndexed { i, log -> 
            if (log.weightKg > 0) weightEntries.add(Entry(i.toFloat(), log.weightKg.toFloat())) 
        }
        if (weightEntries.isNotEmpty()) {
            val dataSet = LineDataSet(weightEntries, "Weight").apply {
                color = ContextCompat.getColor(requireContext(), R.color.green)
                lineWidth = 3f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_gradient_fill)
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    }

    private fun updateCalorieCard(weight: Double, bmi: Double) {
        binding.calorieCard.root.findViewById<TextView>(R.id.tvBmiValue)?.text = String.format("%.1f", bmi)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
