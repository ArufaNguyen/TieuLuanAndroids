package com.example.tieuluanandroids

import android.app.AlertDialog
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tieuluanandroids.model.AppResult
import com.example.tieuluanandroids.model.CreateTagInput
import com.example.tieuluanandroids.model.service.SmartCalendarData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.util.Calendar

class PopupFragment : BottomSheetDialogFragment() {
    private lateinit var textTitle: TextView
    private lateinit var spinnerTopic: Spinner
    private lateinit var btnManageTopics: Button
    private lateinit var editText: EditText
    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button
    private lateinit var saveButton: Button
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private lateinit var adapter: ArrayAdapter<String>

    private val topicList = ArrayList<String>()
    private val topicLocalIds = ArrayList<String?>()
    private var pendingTopicSelection: String? = null
    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data

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
        btnManageTopics = view.findViewById(R.id.btn_manage_topics)
        editText = view.findViewById(R.id.edt_event_content)
        startTimeButton = view.findViewById(R.id.btn_time_start)
        endTimeButton = view.findViewById(R.id.btn_time_end)
        saveButton = view.findViewById(R.id.btn_save_event)
        editButton = view.findViewById(R.id.btn_edit_event)
        deleteButton = view.findViewById(R.id.btn_delete_event)

        val selectedDay = arguments?.getString("KEY_THU") ?: "Monday"
        val selectedTime = normalizeInitialTime(arguments?.getString("KEY_GIO"))
        val selectedEndTime = normalizeInitialTime(arguments?.getString("KEY_GIO_KET_THUC"))
            .takeIf { arguments?.containsKey("KEY_GIO_KET_THUC") == true }
            ?: defaultEndTime(selectedTime)
        val eventLocalId = arguments?.getString("KEY_EVENT_LOCAL_ID")
        val initialTitle = arguments?.getString("KEY_NOI_DUNG").orEmpty()
        val tagNames = arguments?.getStringArrayList("KEY_TAG_NAMES")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            .orEmpty()
        val tagLocalIds = arguments?.getStringArrayList("KEY_TAG_LOCAL_IDS").orEmpty()
        val initialTopic = arguments?.getString("KEY_TOPIC")
        val isEditMode = arguments?.containsKey("EDIT_NOI_DUNG") == true

        setTopics(tagNames, tagLocalIds)
        textTitle.text = if (eventLocalId == null && !isEditMode) {
            "Them su kien ngay $selectedDay"
        } else {
            "Sua su kien ngay $selectedDay"
        }
        startTimeButton.text = if (isEditMode) arguments?.getString("EDIT_BAT_DAU") else selectedTime
        endTimeButton.text = if (isEditMode) arguments?.getString("EDIT_KET_THUC") else selectedEndTime

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, topicList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTopic.adapter = adapter

        selectTopic(initialTopic)
        editText.setText(arguments?.getString("EDIT_NOI_DUNG") ?: stripTopicPrefix(initialTitle, topicList))
        if (isEditMode) {
            saveButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE
        } else {
            saveButton.visibility = View.VISIBLE
            editButton.visibility = View.GONE
            deleteButton.visibility = if (eventLocalId.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        btnManageTopics.setOnClickListener { showCreateTagDialog() }
        startTimeButton.setOnClickListener { showTimePicker(startTimeButton) }
        endTimeButton.setOnClickListener { showTimePicker(endTimeButton) }
        saveButton.setOnClickListener { saveEvent(selectedDay, eventLocalId) }
        editButton.setOnClickListener { editLegacyEvent(selectedDay) }
        deleteButton.setOnClickListener { deleteEvent(selectedDay, eventLocalId) }

        observeTags()
    }

    private fun showCreateTagDialog() {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Them chu de moi")
            .setView(input)
            .setPositiveButton("Them") { _, _ ->
                val newTopic = input.text.toString().trim()
                if (newTopic.isNotEmpty()) createTag(newTopic)
            }
            .setNegativeButton("Huy", null)
            .show()
    }

    private fun saveEvent(selectedDay: String, eventLocalId: String?) {
        val content = editText.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "Vui long nhap noi dung!", Toast.LENGTH_SHORT).show()
            return
        }
        val startTime = startTimeButton.text.toString()
        val endTime = endTimeButton.text.toString()
        parentFragmentManager.setFragmentResult(
            "LUU_SU_KIEN",
            Bundle().apply {
                putString("TRA_VE_THU", selectedDay)
                putString("TRA_VE_GIO", startTime)
                putString("TRA_VE_GIO_BAT_DAU", startTime)
                putString("TRA_VE_GIO_KET_THUC", endTime)
                putString("TRA_VE_NOI_DUNG", content)
                putString("TRA_VE_EVENT_LOCAL_ID", eventLocalId)
                putString("TRA_VE_TAG_LOCAL_ID", selectedTagLocalId())
            }
        )
        Toast.makeText(requireContext(), "Da luu su kien!", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun editLegacyEvent(selectedDay: String) {
        val content = editText.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "Vui long nhap noi dung!", Toast.LENGTH_SHORT).show()
            return
        }
        val startTime = startTimeButton.text.toString()
        val endTime = endTimeButton.text.toString()
        val topic = spinnerTopic.selectedItem?.toString().orEmpty()
        parentFragmentManager.setFragmentResult(
            "SUA_SU_KIEN",
            Bundle().apply {
                putString("TRA_VE_THU", selectedDay)
                putString("TRA_VE_GIO", startTime)
                putString("TRA_VE_GIO_BAT_DAU", startTime)
                putString("TRA_VE_GIO_KET_THUC", endTime)
                putString("TRA_VE_NOI_DUNG", "$topic: $content")
                putString("GOC_NOI_DUNG", arguments?.getString("EDIT_NOI_DUNG"))
                putString("GOC_BAT_DAU", arguments?.getString("EDIT_BAT_DAU"))
                putString("GOC_KET_THUC", arguments?.getString("EDIT_KET_THUC"))
            }
        )
        Toast.makeText(requireContext(), "Da sua lich!", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun deleteEvent(selectedDay: String, eventLocalId: String?) {
        val isEditMode = arguments?.containsKey("EDIT_NOI_DUNG") == true
        if (isEditMode) {
            parentFragmentManager.setFragmentResult(
                "XOA_SU_KIEN",
                Bundle().apply {
                    putString("TRA_VE_THU", selectedDay)
                    putString("GOC_NOI_DUNG", arguments?.getString("EDIT_NOI_DUNG"))
                    putString("GOC_BAT_DAU", arguments?.getString("EDIT_BAT_DAU"))
                    putString("GOC_KET_THUC", arguments?.getString("EDIT_KET_THUC"))
                }
            )
            Toast.makeText(requireContext(), "Da xoa lich!", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }
        val localId = eventLocalId ?: return
        parentFragmentManager.setFragmentResult(
            "XOA_SU_KIEN",
            Bundle().apply { putString("TRA_VE_EVENT_LOCAL_ID", localId) }
        )
        Toast.makeText(requireContext(), "Da xoa lich!", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun observeTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                data.observeTags().collect { tags ->
                    val selectedName = pendingTopicSelection ?: spinnerTopic.selectedItem?.toString()
                    setTopics(tags.map { it.name }, tags.map { it.localId })
                    selectTopic(selectedName)
                    pendingTopicSelection = null
                }
            }
        }
    }

    private fun createTag(name: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = data.createTag(CreateTagInput(name = name))) {
                is AppResult.Success -> {
                    pendingTopicSelection = name
                    Toast.makeText(requireContext(), "Da them tag", Toast.LENGTH_SHORT).show()
                }
                is AppResult.Error -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setTopics(names: List<String>, localIds: List<String>) {
        topicList.clear()
        topicLocalIds.clear()
        val cleanNames = names.map(String::trim).filter(String::isNotBlank)
        if (cleanNames.isEmpty()) {
            topicList.add("Khong co chu de")
            topicLocalIds.add(null)
        } else {
            cleanNames.forEachIndexed { index, name ->
                topicList.add(name)
                topicLocalIds.add(localIds.getOrNull(index))
            }
        }
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }

    private fun selectTopic(topicName: String?) {
        val index = topicName?.let { topic ->
            topicList.indexOfFirst { it.equals(topic, ignoreCase = true) }
        } ?: -1
        if (index >= 0) spinnerTopic.setSelection(index)
    }

    private fun selectedTagLocalId(): String? =
        topicLocalIds.getOrNull(spinnerTopic.selectedItemPosition)

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

    private fun stripTopicPrefix(title: String, topics: List<String>): String {
        val prefix = topics.firstOrNull { title.startsWith("$it:", ignoreCase = true) }
            ?: return title
        return title.drop(prefix.length + 1).trim()
    }
}
