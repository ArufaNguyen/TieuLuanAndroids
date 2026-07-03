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
    private lateinit var timeStartBtn: Button
    private lateinit var timeEndBtn: Button
    private lateinit var saveBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_popup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textTitle = view.findViewById(R.id.text_popup_title)
        spinnerTopic = view.findViewById(R.id.spinner_topic)
        editText = view.findViewById(R.id.edt_event_content)
        timeStartBtn = view.findViewById(R.id.btn_time_start)
        timeEndBtn = view.findViewById(R.id.btn_time_end)
        saveBtn = view.findViewById(R.id.btn_save_event)

        // 1 khoi tao spinner
        val istTopic = arrayOf("Học tập", "Công việc", "Cá nhân", "Giải trí")
        val adapterTopic = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, istTopic)
        adapterTopic.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTopic.adapter = adapterTopic

        // 2 doc du lieu
        val thuDuocChon = arguments?.getString("KEY_THU") ?: "Monday"
        val gioMacDinh = arguments?.getString("KEY_GIO") ?: "00:00"

        val editNoiDung = arguments?.getString("EDIT_NOI_DUNG") ?: ""
        val editBatDau = arguments?.getString("EDIT_BAT_DAU") ?: gioMacDinh
        val editKetThuc = arguments?.getString("EDIT_KET_THUC") ?: ""

        textTitle.text = if (editNoiDung.isEmpty()) "Thêm sự kiện $thuDuocChon" else "Sửa sự kiện $thuDuocChon"

        // 3 sua du nd trong popup
        if (editNoiDung.isNotEmpty()) {
            if (editNoiDung.contains(": ")) {
                val parts = editNoiDung.split(": ", limit = 2)
                val topic = parts[0]
                val content = parts[1]
                editText.setText(content)
                val pos = istTopic.indexOf(topic)
                if (pos >= 0) spinnerTopic.setSelection(pos)
            } else {
                editText.setText(editNoiDung)
            }
        }

        timeStartBtn.text = editBatDau
        timeEndBtn.text = if (editKetThuc.isNotEmpty()) editKetThuc else "Chọn giờ kết thúc"

        // 3 cai nay xu ly gio
        timeStartBtn.setOnClickListener {
            showTimePicker(timeStartBtn)
        }
        timeEndBtn.setOnClickListener {
            showTimePicker(timeEndBtn)
        }

        // 4 luu su kien
        saveBtn.setOnClickListener {
            val content = editText.text.toString().trim()
            val topic = spinnerTopic.selectedItem.toString()
            val timeStart = timeStartBtn.text.toString()
            val timeEnd = timeEndBtn.text.toString()

            if (content.isEmpty() || timeEnd == "Chọn giờ kết thúc") {
                Toast.makeText(requireContext(), "Vui lòng nhập đủ nội dung và thời gian!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resultBundle = Bundle().apply {
                putString("TRA_VE_THU", thuDuocChon)
                putString("TRA_VE_TIME_START", timeStart)
                putString("TRA_VE_TIME_END", timeEnd)
                putString("TRA_VE_NOI_DUNG", "$topic: $content")
            }

            parentFragmentManager.setFragmentResult("LUU_SU_KIEN", resultBundle)
            dismiss()
        }
    }

    private fun showTimePicker(button: Button) {
        val calendar = Calendar.getInstance()
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                button.text = String.format("%02d:%02d", hour, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePicker.show()
    }
}
