package com.example.nutriority.ui.workout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.nutriority.databinding.ActivityAllWorkoutsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AllWorkoutsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllWorkoutsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllWorkoutsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
    }
}