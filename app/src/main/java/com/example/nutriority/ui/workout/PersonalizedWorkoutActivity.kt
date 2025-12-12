package com.example.nutriority.ui.workout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nutriority.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PersonalizedWorkoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personalized_workout)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PersonalizedWorkoutFragment())
                .commit()
        }
    }
}
