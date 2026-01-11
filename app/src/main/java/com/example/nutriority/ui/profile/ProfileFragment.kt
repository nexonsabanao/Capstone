package com.example.nutriority.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nutriority.R
import com.example.nutriority.databinding.FragmentProfileBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupWeightChart()
        observeViewModel()
    }

    private fun setupWeightChart() {
        val chart = binding.root.findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.line_chart) ?: return

        // 1. Create Sample Data (Example: Last 7 days weight)
        val entries = ArrayList<Entry>()
        entries.add(Entry(0f, 65f))
        entries.add(Entry(1f, 64.5f))
        entries.add(Entry(2f, 64.8f))
        entries.add(Entry(3f, 63.2f))
        entries.add(Entry(4f, 62.5f))
        entries.add(Entry(5f, 61.8f))
        entries.add(Entry(6f, 61f))

        val dataSet = LineDataSet(entries, "Weight").apply {
            color = ContextCompat.getColor(requireContext(), R.color.green)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.green))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            circleHoleRadius = 2.5f
            circleHoleColor = Color.WHITE
            valueTextSize = 0f // Hide values on points for cleaner look
            setDrawFilled(true)
            
            // Create Gradient Fill
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_gradient_fill)
            
            mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curves
            setDrawValues(false)
        }

        // 2. Chart Styling
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            extraBottomOffset = 10f
        }

        // 3. X-Axis (Days)
        val labels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(false)
            valueFormatter = IndexAxisValueFormatter(labels)
            textColor = Color.parseColor("#9E9E9E")
            textSize = 10f
            granularity = 1f
            labelCount = 7
        }

        // 4. Y-Axis (Weight)
        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.parseColor("#F5F5F5")
            setDrawAxisLine(false)
            textColor = Color.parseColor("#9E9E9E")
            textSize = 10f
            axisMinimum = 55f
            axisMaximum = 75f
            labelCount = 5
        }
        chart.axisRight.isEnabled = false // Disable right axis

        // 5. Load Data and Animate
        chart.data = LineData(dataSet)
        chart.animateY(1000, Easing.EaseInOutQuad)
        chart.invalidate()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // Future: Observe real weight logs from database
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
