package com.example.nutriority

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.*

class ProfileFragment : Fragment() {

    // UI components
    private lateinit var tvReportTitle: TextView
    private lateinit var tvWorkoutValue: TextView
    private lateinit var tvKcalValue: TextView
    private lateinit var tvMinuteValue: TextView
    private lateinit var tvDayStreak: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var tvCurrentWeight: TextView
    private lateinit var tvHeaviestWeight: TextView
    private lateinit var tvLightestWeight: TextView
    private lateinit var tvHeight: TextView
    private lateinit var tvBMI: TextView
    private lateinit var tvBMIStatus: TextView
    private lateinit var tvDietTitle: TextView
    private lateinit var tvRemainingCalories: TextView
    private lateinit var progressProtein: ProgressBar
    private lateinit var progressFat: ProgressBar
    private lateinit var progressCarbs: ProgressBar
    private lateinit var tvProtein: TextView
    private lateinit var tvFat: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFoodIntake: TextView
    private lateinit var tvBreakfast: TextView
    private lateinit var tvBreakfastCalories: TextView
    private lateinit var tvBreakfastProtein: TextView
    private lateinit var tvLunch: TextView
    private lateinit var tvLunchCalories: TextView
    private lateinit var tvLunchProtein: TextView
    private lateinit var btnLog: Button
    private lateinit var btnEdit: Button

    // New UI components for chart
    private lateinit var weightChart: LineChart
    private lateinit var etWeightInput: EditText
    private lateinit var btnLogWeight: Button

    // Data
    private var workoutCount = 0
    private var caloriesBurned = 0
    private var minutesWorked = 0
    private var dayStreak = 0
    private var currentWeight = 61f
    private var heaviestWeight = 61f
    private var lightestWeight = 54f
    private var height = 165
    private var bmi = 22.4f
    private var bmiStatus = "Healthy weight"
    private var remainingCalories = 1280
    private var foodIntakeCalories = 1145
    private var protein = Pair(88, 182) // current, goal
    private var fat = Pair(65, 54) // current, goal
    private var carbs = Pair(54, 303) // current, goal
    private var breakfast = Meal(
        name = "Hard-Boiled Egg",
        calories = 710,
        protein = 63
    )
    private var lunch = Meal(
        name = "Rice with Meat",
        calories = 435,
        protein = 25
    )

    // Data for the chart
    private val weightEntries = ArrayList<Entry>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        initializeViews(view)

        // Set up the chart with initial data
        setupChart()
        addInitialData()
        updateChart()

        // Set up the UI with data
        updateUI()

        // Set up click listeners
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        // Report section
        tvReportTitle = view.findViewById(R.id.tvReportTitle)
        tvWorkoutValue = view.findViewById(R.id.tvWorkoutValue)
        tvKcalValue = view.findViewById(R.id.tvKcalValue)
        tvMinuteValue = view.findViewById(R.id.tvMinuteValue)
        tvDayStreak = view.findViewById(R.id.tvDayStreak)
        calendarView = view.findViewById(R.id.calendarView)

        // Weight section
        tvCurrentWeight = view.findViewById(R.id.tvCurrentWeight)
        tvHeaviestWeight = view.findViewById(R.id.tvHeaviestWeight)
        tvLightestWeight = view.findViewById(R.id.tvLightestWeight)
        tvHeight = view.findViewById(R.id.tvHeight)

        // Initialize new chart views
        weightChart = view.findViewById(R.id.weightChart)
        etWeightInput = view.findViewById(R.id.etWeightInput)
        btnLogWeight = view.findViewById(R.id.btnLogWeight)

        // BMI section
        tvBMI = view.findViewById(R.id.tvBMI)
        tvBMIStatus = view.findViewById(R.id.tvBMIStatus)

        // Diet tracking section
        tvDietTitle = view.findViewById(R.id.tvDietTitle)
        tvRemainingCalories = view.findViewById(R.id.tvRemainingCalories)

        // Macronutrients
        progressProtein = view.findViewById(R.id.progressProtein)
        progressFat = view.findViewById(R.id.progressFat)
        progressCarbs = view.findViewById(R.id.progressCarbs)
        tvProtein = view.findViewById(R.id.tvProtein)
        tvFat = view.findViewById(R.id.tvFat)
        tvCarbs = view.findViewById(R.id.tvCarbs)

        // Food intake
        tvFoodIntake = view.findViewById(R.id.tvFoodIntake)
        tvBreakfast = view.findViewById(R.id.tvBreakfast)
        tvBreakfastCalories = view.findViewById(R.id.tvBreakfastCalories)
        tvBreakfastProtein = view.findViewById(R.id.tvBreakfastProtein)
        tvLunch = view.findViewById(R.id.tvLunch)
        tvLunchCalories = view.findViewById(R.id.tvLunchCalories)
        tvLunchProtein = view.findViewById(R.id.tvLunchProtein)

        // Buttons
        btnLog = view.findViewById(R.id.btnLog)
        btnEdit = view.findViewById(R.id.btnEdit)
    }

    private fun setupClickListeners() {
        btnLog.setOnClickListener {
            // Handle log button click
            // This could open a dialog to log a new workout or meal
        }

        btnEdit.setOnClickListener {
            // Handle edit button click
            // This could open a dialog to edit the profile information
        }

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Handle date selection
            // This could show the data for the selected date
        }

        // Set click listener for the new Log Weight button
        btnLogWeight.setOnClickListener {
            logWeight()
        }
    }

    private fun updateUI() {
        // Update report section
        tvWorkoutValue.text = workoutCount.toString()
        tvKcalValue.text = caloriesBurned.toString()
        tvMinuteValue.text = minutesWorked.toString()
        tvDayStreak.text = "Day Streak $dayStreak"

        // Update weight section
        tvCurrentWeight.text = "$currentWeight kg"
        tvHeaviestWeight.text = "$heaviestWeight kg"
        tvLightestWeight.text = "$lightestWeight kg"
        tvHeight.text = "$height cm"

        // Update BMI section
        tvBMI.text = String.format("%.1f", bmi) // Format to 1 decimal place
        tvBMIStatus.text = " ($bmiStatus)"

        // Update diet tracking section
        tvRemainingCalories.text = "$remainingCalories kcal"

        // Update macronutrients
        progressProtein.max = protein.second
        progressProtein.progress = protein.first
        tvProtein.text = "${protein.first}/${protein.second}g"

        progressFat.max = fat.second
        progressFat.progress = fat.first
        tvFat.text = "${fat.first}/${fat.second}g"

        progressCarbs.max = carbs.second
        progressCarbs.progress = carbs.first
        tvCarbs.text = "${carbs.first}/${carbs.second}g"

        // Update food intake
        tvFoodIntake.text = "Food intake $foodIntakeCalories kcal"

        tvBreakfast.text = breakfast.name
        tvBreakfastCalories.text = "${breakfast.calories} kcal"
        tvBreakfastProtein.text = "${breakfast.protein}g protein"

        tvLunch.text = lunch.name
        tvLunchCalories.text = "${lunch.calories} kcal"
        tvLunchProtein.text = "${lunch.protein}g protein"
    }

    // New function to handle logging weight
    private fun logWeight() {
        val weightString = etWeightInput.text.toString()

        if (weightString.isEmpty()) {
            Toast.makeText(context, "Please enter a weight", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val weight = weightString.toFloat()
            // Use the current size of the list as the x-value to create a sequential timeline
            val xValue = if (weightEntries.isNotEmpty()) weightEntries.last().x + 1f else 1f

            // Add the new entry to our list
            weightEntries.add(Entry(xValue, weight))

            // Update the chart and stats
            updateChart()
            updateWeight(weight) // Reuse the existing function to update stats and UI

            // Clear the input field
            etWeightInput.text.clear()

            Toast.makeText(context, "Weight logged successfully!", Toast.LENGTH_SHORT).show()

        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid weight format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupChart() {
        // General chart styling
        weightChart.setTouchEnabled(true)
        weightChart.setPinchZoom(true)
        weightChart.description.isEnabled = false
        weightChart.legend.isEnabled = false // Hide legend

        // Styling the X-axis
        weightChart.xAxis.setDrawGridLines(false)
        weightChart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM

        // Styling the Y-axis (left)
        weightChart.axisLeft.setDrawGridLines(true)
        weightChart.axisLeft.axisMinimum = 50f // Set a min Y value
        weightChart.axisLeft.axisMaximum = 80f // Set a max Y value

        // Disable the right Y-axis
        weightChart.axisRight.isEnabled = false
    }

    private fun addInitialData() {
        // Add some sample data points
        // In a real app, you would load this from a database or SharedPreferences
        weightEntries.add(Entry(1f, 62.5f))
        weightEntries.add(Entry(2f, 62.1f))
        weightEntries.add(Entry(3f, 61.8f))
        weightEntries.add(Entry(4f, 61.9f))
        weightEntries.add(Entry(5f, 61.5f))
    }

    private fun updateChart() {
        // Create a dataset from the entries
        val dataSet = LineDataSet(weightEntries, "Weight Data")
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(true)
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(context?.getColor(R.color.purple_500) ?: 0) // Use a color from your resources
        dataSet.color = context?.getColor(R.color.purple_500) ?: 0
        dataSet.lineWidth = 2f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Makes the line curved

        // Create a LineData object
        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(dataSet)
        val lineData = LineData(dataSets)

        // Set the data and refresh
        weightChart.data = lineData
        weightChart.notifyDataSetChanged()
        weightChart.invalidate()
    }

    fun updateWeight(newWeight: Float) {
        currentWeight = newWeight

        // Update heaviest or lightest if needed
        if (newWeight > heaviestWeight) {
            heaviestWeight = newWeight
        }

        if (newWeight < lightestWeight) {
            lightestWeight = newWeight
        }

        // Recalculate BMI
        calculateBMI()

        // Update UI
        updateUI()
    }

    private fun calculateBMI() {
        val heightInMeters = height / 100f

        if (heightInMeters > 0) {
            bmi = currentWeight / (heightInMeters * heightInMeters)

            // Determine BMI status
            bmiStatus = when {
                bmi < 18.5 -> "Underweight"
                bmi < 25 -> "Healthy weight"
                bmi < 30 -> "Overweight"
                else -> "Obese"
            }
        }
    }

    // ... (keep the rest of your existing functions like logWorkout, addMeal, etc.) ...
    fun logWorkout(calories: Int, minutes: Int) {
        caloriesBurned += calories
        minutesWorked += minutes
        workoutCount += 1

        // Update remaining calories
        remainingCalories += calories

        // Update UI
        updateUI()
    }

    fun addMeal(meal: Meal, mealType: MealType) {
        when (mealType) {
            MealType.BREAKFAST -> breakfast = meal
            MealType.LUNCH -> lunch = meal
            MealType.DINNER -> {
                // Add dinner meal if needed
            }
            MealType.SNACK -> {
                // Add snack meal if needed
            }
        }

        // Update food intake
        foodIntakeCalories += meal.calories

        // Update remaining calories
        remainingCalories -= meal.calories

        // Update macronutrients
        protein = Pair(protein.first + meal.protein, protein.second)
        fat = Pair(fat.first + meal.fat, fat.second)
        carbs = Pair(carbs.first + meal.carbs, carbs.second)

        // Update UI
        updateUI()
    }

    fun incrementDayStreak() {
        dayStreak += 1
        updateUI()
    }

    fun resetDayStreak() {
        dayStreak = 0
        updateUI()
    }
}

data class Meal(
    val name: String,
    val calories: Int,
    val protein: Int,
    val fat: Int = 0,
    val carbs: Int = 0
)

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}