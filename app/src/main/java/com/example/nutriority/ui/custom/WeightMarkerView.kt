package com.example.nutriority.ui.custom

import android.content.Context
import android.widget.TextView
import com.example.nutriority.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

class WeightMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvWeight: TextView = findViewById(R.id.tv_marker_weight)
    private val tvDate: TextView = findViewById(R.id.tv_marker_date)
    private val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            tvWeight.text = String.format("%.1f", it.y)
            
            // Extract the date from the entry's data property
            val dateMillis = it.data as? Long
            if (dateMillis != null) {
                tvDate.text = sdf.format(Date(dateMillis))
            } else {
                tvDate.text = ""
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat() - 20f)
    }
}
