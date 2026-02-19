package com.example.nutriority.ui.util

import android.app.DatePickerDialog
import android.content.Context
import java.util.Calendar

object DatePickerUtil {
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
                onDateSelected(selectedCalendar.timeInMillis)
            },
            year, month, day
        )
        
        // --- AGE RESTRICTION --- 
        val today = Calendar.getInstance()

        // Max date: 17 years ago from today
        val maxDate = Calendar.getInstance().apply {
            add(Calendar.YEAR, -17)
        }
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        // Min date: 28 years ago from today
        val minDate = Calendar.getInstance().apply {
            add(Calendar.YEAR, -28)
        }
        datePickerDialog.datePicker.minDate = minDate.timeInMillis

        // Set initial display to a valid date within the range (e.g., 22 years old)
        if (initialDateMillis == null) {
            val initial = Calendar.getInstance().apply { add(Calendar.YEAR, -22) }
            datePickerDialog.updateDate(initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH))
        }

        datePickerDialog.show()
    }
}
