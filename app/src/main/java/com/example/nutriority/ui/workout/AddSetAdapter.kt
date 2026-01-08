package com.example.nutriority.ui.workout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.databinding.ItemAddSetBinding

class AddSetAdapter(
    private val onAddSetClick: () -> Unit
) : RecyclerView.Adapter<AddSetAdapter.AddSetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddSetViewHolder {
        val binding = ItemAddSetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddSetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddSetViewHolder, position: Int) {
        // No data to bind, but the click listener is set in the ViewHolder.
    }

    override fun getItemCount(): Int = 1

    inner class AddSetViewHolder(binding: ItemAddSetBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onAddSetClick()
            }
        }
    }
}
