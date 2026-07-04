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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tieuluanandroids.model.Tag
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
    private lateinit var deleteButton: Button

    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data

    // ArrayList động chứa danh sách chủ đề hiện có (thay cho mảng String[] cố định trước đây)
    private val danhSachChuDe: ArrayList<Tag> = ArrayList()
    private lateinit var tagAdapter: ArrayAdapter<String>

    private var eventLocalId: String? = null
    private var selectedTagLocalId: String? = null

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
        deleteButton = view.findViewById(R.id.btn_delete_event)

        val selectedDay = arguments?.getString("KEY_THU") ?: "Monday"
        val selectedTime = normalizeInitialTime(arguments?.getString("KEY_GIO"))
        val selectedEndTime = normalizeInitialTime(arguments?.getString("KEY_GIO_KET_THUC"))
            .takeIf { arguments?.containsKey("KEY_GIO_KET_THUC") == true }
            ?: defaultEndTime(selectedTime)
        eventLocalId = arguments?.getString("KEY_EVENT_LOCAL_ID")
        val initialTitle = arguments?.getString("KEY_NOI_DUNG").orEmpty()
        val initialTagLocalId = arguments?.getString("KEY_TAG_LOCAL_ID")
        selectedTagLocalId = initialTagLocalId

        // Sửa lịch nếu ô đã có sự kiện (eventLocalId != null), ngược lại là Thêm lịch mới
        val dangSua = !eventLocalId.isNullOrBlank()
        textTitle.text = if (!dangSua) "Thêm lịch ngày $selectedDay" else "Sửa lịch ngày $selectedDay"
        saveButton.text = if (!dangSua) "Lưu lịch" else "Sửa lịch"
        startTimeButton.text = selectedTime
        endTimeButton.text = selectedEndTime

        // Spinner chủ đề khởi tạo rỗng, sẽ được nạp động (ArrayList) ngay khi Room trả dữ liệu
        tagAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf("-")
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerTopic.adapter = tagAdapter

        editText.setText(initialTitle)
        deleteButton.visibility = if (dangSua) View.VISIBLE else View.GONE

        // Mở giao diện quản lý chủ đề (Thêm/Sửa/Xóa chủ đề tự chọn)
        btnManageTopics.setOnClickListener {
            TagManagerDialogFragment().show(parentFragmentManager, "tag_manager")
        }

        // Theo dõi Room theo thời gian thực -> tự cập nhật ArrayList & Spinner khi có Thêm/Sửa/Xóa chủ đề
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                data.observeTags().collect { tags ->
                    capNhatSpinnerChuDe(tags)
                }
            }
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

            val tagIndex = spinnerTopic.selectedItemPosition
            val selectedTagLocalIdMoi = danhSachChuDe.getOrNull(tagIndex)?.localId
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
                    putString("TRA_VE_TAG_LOCAL_ID", selectedTagLocalIdMoi)
                }
            )

            Toast.makeText(
                requireContext(),
                if (!dangSua) "Da luu lich!" else "Da sua lich!",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }

        // Nút Xóa lịch: xóa sự kiện của ô hiện tại. Sau khi Room xác nhận xóa thành công,
        // CalendarFragment sẽ tự động gọi lại cell.removeAllViews() (trong clearCalendarCells())
        // và vẽ lại lịch mới nhất thông qua luồng Flow observeEvents().
        deleteButton.setOnClickListener {
            val localId = eventLocalId ?: return@setOnClickListener
            parentFragmentManager.setFragmentResult(
                "XOA_SU_KIEN",
                Bundle().apply {
                    putString("TRA_VE_EVENT_LOCAL_ID", localId)
                }
            )
            Toast.makeText(requireContext(), "Da xoa lich!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    /** Nạp lại ArrayList chủ đề động vào Spinner, giữ lại lựa chọn hiện tại nếu còn tồn tại */
    private fun capNhatSpinnerChuDe(tags: List<Tag>) {
        danhSachChuDe.clear()
        danhSachChuDe.addAll(tags)

        val tenChuDe = if (danhSachChuDe.isEmpty()) listOf("-") else danhSachChuDe.map { it.name }
        tagAdapter.clear()
        tagAdapter.addAll(tenChuDe)
        tagAdapter.notifyDataSetChanged()

        // Ưu tiên giữ lựa chọn theo tagLocalId ban đầu (khi Sửa lịch)
        val viTriChon = danhSachChuDe.indexOfFirst { it.localId == selectedTagLocalId }
        if (viTriChon >= 0) {
            spinnerTopic.setSelection(viTriChon)
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
        return "08:00"
    }

    private fun defaultEndTime(startTime: String): String {
        val parts = startTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return String.format("%02d:%02d", (hour + 1) % 24, minute)
    }
}