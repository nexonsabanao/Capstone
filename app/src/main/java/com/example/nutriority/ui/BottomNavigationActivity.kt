package com.example.nutriority.ui

import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nutriority.R
import com.example.nutriority.ui.home.HomeFragment
import com.example.nutriority.ui.meal.MealFragment
import com.example.nutriority.ui.profile.ProfileFragment
import com.example.nutriority.ui.workout.WorkoutFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayDeque

@AndroidEntryPoint
class BottomNavigationActivity : AppCompatActivity() {
    private val fragmentMap = mutableMapOf<String, Fragment>()
    private lateinit var activeFragment: Fragment
    private val backStack = ArrayDeque<String>() // Use tags in backstack

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_bottom)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)

        if (savedInstanceState == null) {
            // First time creation
            val homeFragment = HomeFragment()
            val mealFragment = MealFragment()
            val workoutFragment = WorkoutFragment()
            val profileFragment = ProfileFragment()

            fragmentMap["0"] = homeFragment
            fragmentMap["1"] = mealFragment
            fragmentMap["2"] = workoutFragment
            fragmentMap["3"] = profileFragment

            supportFragmentManager.beginTransaction().apply {
                add(R.id.nav_host_fragment, profileFragment, "3").hide(profileFragment)
                add(R.id.nav_host_fragment, workoutFragment, "2").hide(workoutFragment)
                add(R.id.nav_host_fragment, mealFragment, "1").hide(mealFragment)
                add(R.id.nav_host_fragment, homeFragment, "0")
            }.commit()
            activeFragment = homeFragment
            backStack.push("0")
        } else {
            // Re-creation
            // Fragments are restored by FragmentManager. Let's get them.
            fragmentMap["0"] = supportFragmentManager.findFragmentByTag("0")!!
            fragmentMap["1"] = supportFragmentManager.findFragmentByTag("1")!!
            fragmentMap["2"] = supportFragmentManager.findFragmentByTag("2")!!
            fragmentMap["3"] = supportFragmentManager.findFragmentByTag("3")!!

            // Restore backstack
            savedInstanceState.getStringArrayList("backStack")?.let {
                backStack.addAll(it)
            }
            // Restore active fragment
            val activeTag = savedInstanceState.getString("activeFragmentTag") ?: "0"
            activeFragment = fragmentMap[activeTag]!!
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val tag = getTagForMenuItem(item.itemId)
            val fragment = fragmentMap[tag]

            if (fragment != null && fragment != activeFragment) {
                showFragment(fragment)
                backStack.remove(tag)
                backStack.push(tag)
            }
            true
        }

        onBackPressedDispatcher.addCallback(this) {
            if (backStack.size > 1) {
                backStack.pop()
                val previousTag = backStack.peek()!!
                val previousFragment = fragmentMap[previousTag]!!
                showFragment(previousFragment)
                bottomNavigationView.selectedItemId = getMenuItemForTag(previousTag)
            } else {
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList("backStack", ArrayList(backStack))
        outState.putString("activeFragmentTag", activeFragment.tag)
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }

    private fun getTagForMenuItem(itemId: Int): String {
        return when (itemId) {
            R.id.navigation_home -> "0"
            R.id.navigation_meal -> "1"
            R.id.navigation_workout -> "2"
            R.id.navigation_profile -> "3"
            else -> "0"
        }
    }

    private fun getMenuItemForTag(tag: String): Int {
        return when (tag) {
            "0" -> R.id.navigation_home
            "1" -> R.id.navigation_meal
            "2" -> R.id.navigation_workout
            "3" -> R.id.navigation_profile
            else -> R.id.navigation_home
        }
    }
}
