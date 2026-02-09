package com.example.nutriority

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import com.example.nutriority.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.scottyab.rootbeer.RootBeer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Enable true Edge-to-Edge before super.onCreate() to allow content to overlap system bars
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        if (isDeviceRooted()) {
            showRootedWarning()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // We removed the root padding here. This allows your app's background and content 
        // to truly overlap the status bar and navigation bar for a seamless look.
        // Individual fragments can handle their own specific padding if needed.

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun isDeviceRooted(): Boolean = RootBeer(this).isRooted

    private fun showRootedWarning() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Security Warning")
            .setMessage("This device appears to be rooted. For security reasons, Nutriority cannot run on rooted devices.")
            .setCancelable(false)
            .setPositiveButton("Exit") { _, _ -> finishAffinity() }
            .show()
    }
}
