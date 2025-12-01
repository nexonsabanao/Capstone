package com.example.nutriority

import android.os.Bundle
import com.example.nutriority.BaseFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class WorkoutFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_workout, container, false)
    }

    // Insets handled by BaseFragment
}
