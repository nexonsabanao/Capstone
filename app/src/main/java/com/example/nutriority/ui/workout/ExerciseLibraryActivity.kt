package com.example.nutriority.ui.workout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.nutriority.databinding.ActivityExerciseLibraryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExerciseLibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseLibraryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
    }
}