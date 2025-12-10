package com.example.nutriority.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nutriority.ui.home.HomeFragment
import com.example.nutriority.ui.meal.MealFragment
import com.example.nutriority.ui.profile.ProfileFragment
import com.example.nutriority.R
import com.example.nutriority.ui.workout.WorkoutFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val mealFragment = MealFragment()
    private val workoutFragment = WorkoutFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_bottom)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)

        supportFragmentManager.beginTransaction().apply {
            add(R.id.nav_host_fragment, profileFragment, "3").hide(profileFragment)
            add(R.id.nav_host_fragment, workoutFragment, "2").hide(workoutFragment)
            add(R.id.nav_host_fragment, mealFragment, "1").hide(mealFragment)
            add(R.id.nav_host_fragment, homeFragment, "0")
        }.commit()

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> showFragment(homeFragment)
                R.id.navigation_meal -> showFragment(mealFragment)
                R.id.navigation_workout -> showFragment(workoutFragment)
                R.id.navigation_profile -> showFragment(profileFragment)
            }
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }
}