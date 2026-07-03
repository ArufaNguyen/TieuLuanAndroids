package com.example.tieuluanandroids

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar

class PopupFragment : BottomSheetDialogFragment() {

    private lateinit var textTitle: TextView
    private lateinit var spinnerTopic: Spinner
    private lateinit var editText: EditText
    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button
    private lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_popup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textTitle = view.findViewById(R.id.text_popup_title)
        spinnerTopic = view.findViewById(R.id.spinner_topic)
        editText = view.findViewById(R.id.edt_event_content)
        startTimeButton = view.findViewById(R.id.btn_time_start)
        endTimeButton = view.findViewById(R.id.btn_time_end)
        saveButton = view.findViewById(R.id.btn_save_event)

        val selectedDay = arguments?.getString("KEY_THU") ?: "Monday"
        val selectedTime = normalizeInitialTime(arguments?.getString("KEY_GIO"))

        textTitle.text = "Them su kien ngay $selectedDay"
        startTimeButton.text = selectedTime
        endTimeButton.text = defaultEndTime(selectedTime)

        spinnerTopic.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            arrayOf("Hoc tap", "Cong viec", "Ca nhan", "Giai tri")
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        startTimeButton.setOnClickListener {
            showTimePicker(startTimeButton)
        }

        endTimeButton.setOnClickListener {
            showTimePicker(endTimeButton)
        }

        saveButton.setOnClickListener {
            val content = editText.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Vui long nhap noi dung!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val topic = spinnerTopic.selectedItem.toString()
            val startTime = startTimeButton.text.toString()
            val endTime = endTimeButton.text.toString()

            parentFragmentManager.setFragmentResult(
                "LUU_SU_KIEN",
                Bundle().apply {
                    putString("TRA_VE_THU", selectedDay)
                    putString("TRA_VE_GIO", startTime)
                    putString("TRA_VE_GIO_BAT_DAU", startTime)
                    putString("TRA_VE_GIO_KET_THUC", endTime)
                    putString("TRA_VE_NOI_DUNG", "$topic: $content")
                }
            )

            Toast.makeText(requireContext(), "Da luu su kien!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun showTimePicker(targetButton: Button) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                targetButton.text = String.format("%02d:%02d", hourOfDay, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun normalizeInitialTime(value: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.matches(Regex("\\d{2}:\\d{2}"))) return trimmed
        if (trimmed.matches(Regex("\\d{1}:\\d{2}"))) return "0$trimmed"
        parseAmPmTime(trimmed)?.let { return it }
        return "08:00"
    }

    private fun parseAmPmTime(value: String): String? {
        val match = Regex("""^(\d{1,2})(?::(\d{2}))?\s*([AaPp][Mm])$""").matchEntire(value)
            ?: return null
        val rawHour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
        val marker = match.groupValues[3].uppercase()
        val hour = when {
            marker == "AM" && rawHour == 12 -> 0
            marker == "AM" -> rawHour
            marker == "PM" && rawHour == 12 -> 12
            else -> rawHour + 12
        }
        return String.format("%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    private fun defaultEndTime(startTime: String): String {
        val parts = startTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return String.format("%02d:%02d", (hour + 1) % 24, minute)
    }
}
