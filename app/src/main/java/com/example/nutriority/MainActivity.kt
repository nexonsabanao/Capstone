package com.example.nutriority

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * MainActivity is the app's entry point.
 * Its ONLY responsibility is to display the startup navigation graph (`my_nav.xml`),
 * which begins with the SplashFragment. It does not handle any other UI or logic.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        //splash screen.
        installSplashScreen()
        // Force to light mode
        super.onCreate(savedInstanceState)

        if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {

            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            recreate()

            return
        }
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

    }
}
