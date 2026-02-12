package com.example.nutriority

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import com.example.nutriority.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.scottyab.rootbeer.RootBeer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install SplashScreen before super.onCreate
        installSplashScreen()
        
        super.onCreate(savedInstanceState)

        // Force edge-to-edge
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        if (isDeviceRooted()) {
            showRootedWarning()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }

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
