package com.example.nutriority

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.nutriority.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            recreate()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        // Link the BottomNavigationView with the NavController
        binding.bottomNavigationView.setupWithNavController(navController)

        // Control visibility of Bottom Navigation based on current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isMainTab = when (destination.id) {
                R.id.navigation_home, 
                R.id.navigation_workout, 
                R.id.navigation_meal, 
                R.id.navigation_profile -> true
                else -> false
            }
            
            if (isMainTab) {
                binding.bottomNavigationView.visibility = View.VISIBLE
                binding.separator.visibility = View.VISIBLE
            } else {
                binding.bottomNavigationView.visibility = View.GONE
                binding.separator.visibility = View.GONE
            }
        }
    }
}
