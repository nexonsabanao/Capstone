package com.example.nutriority.ui.onboarding.screens

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.example.nutriority.R
import com.example.nutriority.databinding.DialogDietDetailsBinding

class DietDetailsDialogFragment : DialogFragment() {

    private var _binding: DialogDietDetailsBinding? = null
    private val binding get() = _binding!!

    // Optional listener if you need to perform an action after the dialog closes.
    var onDismissListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDietDetailsBinding.inflate(inflater, container, false)

        // Make the dialog background transparent to show the card's rounded corners.
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Set the dialog's width to use a percentage of the screen width.
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve arguments passed from FourthScreen.
        val title = arguments?.getString(ARG_TITLE)
        // Retrieve the formatted text as a CharSequence.
        val description = arguments?.getCharSequence(ARG_DESCRIPTION)

        // Populate the views.
        binding.dialogTitle.text = title
        binding.dialogDescription.text = description

        // Set an icon based on the title.
        when {
            title?.contains("Balanced") == true -> binding.dialogIcon.setImageResource(R.drawable.img_balanced_diet)
            title?.contains("Low-Carb") == true -> binding.dialogIcon.setImageResource(R.drawable.img_lowcarb_diet)
            title?.contains("Vegetarian") == true -> binding.dialogIcon.setImageResource(R.drawable.img_vegetarian_diet)
        }

        // Handle the button click.
        binding.dialogOkButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStop() {
        super.onStop()
        // Trigger the callback when the dialog is dismissed or closed.
        onDismissListener?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"

        /**
         * Creates a new instance of the dialog.
         * @param title The plain text title.
         * @param description The formatted Spanned/CharSequence object containing HTML styling.
         */
        fun newInstance(title: String, description: Spanned): DietDetailsDialogFragment {
            val fragment = DietDetailsDialogFragment()
            val args = Bundle().apply {
                putString(ARG_TITLE, title)
                // Use putCharSequence to preserve the text formatting.
                putCharSequence(ARG_DESCRIPTION, description)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
