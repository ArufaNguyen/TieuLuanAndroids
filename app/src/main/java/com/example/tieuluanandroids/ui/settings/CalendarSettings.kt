package com.example.tieuluanandroids.ui.settings

import android.content.Context

object CalendarSettings {
    const val MODE_BOX = "box"
    const val MODE_LINE = "line"

    private const val PREFS_NAME = "calendar_settings"
    private const val KEY_WEEKLY_CALENDAR_MODE = "weekly_calendar_mode"

    fun getWeeklyCalendarMode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WEEKLY_CALENDAR_MODE, MODE_LINE)
            ?: MODE_LINE
    }

    fun setWeeklyCalendarMode(context: Context, mode: String) {
        val safeMode = if (mode == MODE_BOX) MODE_BOX else MODE_LINE
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEEKLY_CALENDAR_MODE, safeMode)
            .apply()
    }
}
