package com.example.nutriority.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutriority.databinding.ItemSetBinding

class SetsAdapter : ListAdapter<Int, SetsAdapter.SetViewHolder>(SetDiffCallback()) {

    class SetViewHolder(val binding: ItemSetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSetBinding.inflate(inflater, parent, false)
        return SetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        val reps = getItem(position)
        holder.binding.setNumber.text = (position + 1).toString()
        holder.binding.repsText.text = "$reps\nrep"
    }

    class SetDiffCallback : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }
    }
}
