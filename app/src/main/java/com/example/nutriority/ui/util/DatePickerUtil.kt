package com.example.nutriority.ui.util

import android.app.DatePickerDialog
import android.content.Context
import java.util.Calendar

object DatePickerUtil {
    /**
     * Shows a standard DatePickerDialog.
     * @param context The context to show the dialog in.
     * @param initialDateMillis The initial date to show in the picker. If null, defaults to today.
     * @param onDateSelected Callback invoked when a date is picked, returning the time in milliseconds.
     */
    fun showDatePicker(
        context: Context,
        initialDateMillis: Long?,
        onDateSelected: (Long) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        initialDateMillis?.let { calendar.timeInMillis = it }
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                // Ensure time components are cleared for a "pure" date if needed, 
                // but standard picker sets them to 0 anyway.
                onDateSelected(selectedCalendar.timeInMillis)
            },
            year, month, day
        )
        
        // Prevent selecting future dates (typical for birthdays)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
}
