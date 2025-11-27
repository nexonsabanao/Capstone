package com.example.nutriority.onboarding.screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.R

class PickerAdapter(
    // Change 'val' to 'var' to make the list mutable
    private var numbers: List<String>
) : RecyclerView.Adapter<PickerAdapter.ViewHolder>() {

    // --- NEW FUNCTION TO UPDATE THE DATA ---
    fun updateData(newNumbers: List<String>) {
        this.numbers = newNumbers
        // This tells the RecyclerView to redraw itself with the new data
        notifyDataSetChanged()
    }
    // -----------------------------------------

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.number_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_picker_number, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = numbers[position]
    }

    override fun getItemCount() = numbers.size
}
