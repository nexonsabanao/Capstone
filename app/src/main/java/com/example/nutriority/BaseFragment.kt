package com.example.nutriority

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.nutriority.utils.applySystemBarsInsets

/**
 * Simple base fragment that applies system bar insets to the fragment root view.
 * Fragments that want a different inset behavior can still apply explicit insets
 * or override onViewCreated.
 */
open class BaseFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarsInsets()
    }
}