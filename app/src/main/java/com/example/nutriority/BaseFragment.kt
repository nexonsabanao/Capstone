package com.example.nutriority

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Base fragment that applies system bar insets and consistent padding.
 * Applies top/bottom padding similar to workout fragment (20dp top, 24dp bottom)
 * plus system bar insets for status bar and navigation bar.
 */
open class BaseFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Apply system insets + base padding for consistent spacing
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = v.resources.displayMetrics.density
            // Use existing XML padding as base if present, otherwise fall back
            // to the design defaults (20dp top / 24dp bottom).
            val baseTopPx = if (v.paddingTop > 0) v.paddingTop else (20 * density).toInt()
            val baseBottomPx = if (v.paddingBottom > 0) v.paddingBottom else (24 * density).toInt()
            val topPadding = baseTopPx + systemBars.top
            val bottomPadding = baseBottomPx + systemBars.bottom
            v.updatePadding(top = topPadding, bottom = bottomPadding)
            insets
        }
    }
}
