package com.example.tieuluanandroids.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.example.tieuluanandroids.R
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.settings_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val modeGroup = view.findViewById<RadioGroup>(R.id.radio_weekly_calendar_mode)
        modeGroup.check(
            when (CalendarSettings.getWeeklyCalendarMode(requireContext())) {
                CalendarSettings.MODE_BOX -> R.id.radio_calendar_box_mode
                else -> R.id.radio_calendar_line_mode
            }
        )
        modeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radio_calendar_box_mode -> CalendarSettings.MODE_BOX
                else -> CalendarSettings.MODE_LINE
            }
            CalendarSettings.setWeeklyCalendarMode(requireContext(), mode)
            Snackbar.make(view, "Weekly Calendar Mode updated", Snackbar.LENGTH_SHORT).show()
        }
    }
}
