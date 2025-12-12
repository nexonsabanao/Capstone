package com.example.nutriority.ui.workout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutriority.R
import com.example.nutriority.databinding.ActivityExerciseDetailBinding
import com.example.nutriority.ui.adapter.SetsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseDetailBinding
    private val viewModel: ExerciseDetailViewModel by viewModels()
    private lateinit var setsAdapter: SetsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)

        val exerciseId = intent.getIntExtra("exercise_id", -1)
        if (exerciseId != -1) {
            viewModel.getExerciseById(exerciseId)
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        setsAdapter = SetsAdapter()
        binding.setsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExerciseDetailActivity)
            adapter = setsAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.exercise.collect { exercise ->
                exercise?.let {
                    supportActionBar?.title = it.name
                    val sets = List(it.sets) { _ -> it.reps }
                    setsAdapter.submitList(sets)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
