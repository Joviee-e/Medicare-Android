package com.example.medicare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.widget.DatePicker
import android.widget.TimePicker

object DialogHelper {

    fun createDatePickerDialog(
        context: Context,
        onDateSet: (view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) -> Unit,
        year: Int,
        month: Int,
        dayOfMonth: Int
    ): DatePickerDialog {
        val picker = DatePickerDialog(context, R.style.Theme_Medicare_Dialog, onDateSet, year, month, dayOfMonth)
        picker.setOnShowListener {
            styleDialogButtons(context, picker)
        }
        return picker
    }

    fun createTimePickerDialog(
        context: Context,
        onTimeSet: (view: TimePicker?, hourOfDay: Int, minute: Int) -> Unit,
        hourOfDay: Int,
        minute: Int,
        is24HourView: Boolean
    ): TimePickerDialog {
        val picker = TimePickerDialog(context, R.style.Theme_Medicare_Dialog, onTimeSet, hourOfDay, minute, is24HourView)
        picker.setOnShowListener {
            styleDialogButtons(context, picker)
        }
        return picker
    }

    fun styleDialogButtons(context: Context, dialog: android.app.AlertDialog) {
        val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val buttonColor = if (isNight) Color.parseColor("#4DD0E1") else Color.parseColor("#004D60")
        
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(buttonColor)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 15f
        }
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(buttonColor)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 15f
        }
        dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)?.apply {
            setTextColor(buttonColor)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 15f
        }
    }

    fun styleDialogButtons(context: Context, dialog: androidx.appcompat.app.AlertDialog) {
        val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val buttonColor = if (isNight) Color.parseColor("#4DD0E1") else Color.parseColor("#004D60")
        
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(buttonColor)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 15f
        }
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(buttonColor)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 15f
        }
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)?.apply {
            setTextColor(buttonColor)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 15f
        }
    }
}
