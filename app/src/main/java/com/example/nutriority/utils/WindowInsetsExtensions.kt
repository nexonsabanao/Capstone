package com.example.nutriority.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Small helper to apply system bar insets (status + navigation) to a view's top/bottom padding.
 * Keeps existing start/end padding intact and replaces top/bottom with the system inset sizes.
 */
fun View.applySystemBarsInsets(applyTop: Boolean = true, applyBottom: Boolean = true) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val newTop = if (applyTop) systemBars.top else v.paddingTop
        val newBottom = if (applyBottom) systemBars.bottom else v.paddingBottom
        v.updatePadding(top = newTop, bottom = newBottom)
        insets
    }
}

/**
 * Convenience to only apply status bar inset at top (for screens that only need top inset).
 */
fun View.applyStatusBarInset() = applySystemBarsInsets(applyTop = true, applyBottom = false)
