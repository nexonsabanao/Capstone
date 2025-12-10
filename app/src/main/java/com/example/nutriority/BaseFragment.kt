package com.example.nutriority

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.nutriority.utils.applySystemBarsInsets
open class BaseFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarsInsets()
    }
}