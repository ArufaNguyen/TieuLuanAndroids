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

    /* b1: khai bao bien toan cuc (Đặt gạch xí chỗ) */
    private lateinit var textTitle: TextView
    private lateinit var spinnerTopic: Spinner
    private lateinit var editText: EditText
    private lateinit var timeBtn: Button
    private lateinit var saveBtn: Button

    /* b2: nap xml (Thổi giao diện vào code) */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_popup, container, false)
    }

    /* b3: anh xa cac su kien (nối dây điện) */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textTitle = view.findViewById<TextView>(R.id.text_popup_title)
        spinnerTopic = view.findViewById<Spinner>(R.id.spinner_topic)
        editText = view.findViewById<EditText>(R.id.edt_event_content)
        timeBtn = view.findViewById<Button>(R.id.btn_edt_time)
        saveBtn = view.findViewById<Button>(R.id.btn_save_event)

        /* b4: viet code thuc thi (Logic chức năng) */

        // ĐỌC HÀNH LÝ ĐƯỢC GỬI SANG (Lấy thông tin ô vừa bấm)
        val thuDuocChon = arguments?.getString("KEY_THU") ?: "Mon"
        val gioDuocChon = arguments?.getString("KEY_GIO") ?: "Chọn giờ"

        timeBtn.text = gioDuocChon

        textTitle.text = "Thêm sự kiện ngày $thuDuocChon"


        val istTopic = arrayOf("Học tập", "Công việc", "Cá nhân", "Giải trí")
        val adapterTopic = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, istTopic)
        adapterTopic.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTopic.adapter = adapterTopic //

        timeBtn.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                    timeBtn.text = selectedTime
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePickerDialog.show()
        }

        // 3. XỬ LÝ LƯU SỰ KIỆN (Bấm nút Save)
        saveBtn.setOnClickListener { //
            val content = editText.text.toString().trim() //
            val topic = spinnerTopic.selectedItem.toString() //
            val time = timeBtn.text.toString() // Giờ người dùng chọn (Ví dụ: "1 AM" hoặc "12 PM")

            if (content.isEmpty()) { //
                Toast.makeText(requireContext(), "Vui lòng nhập nội dung!", Toast.LENGTH_SHORT).show() //
                return@setOnClickListener //
            }

            // Đọc Thứ được truyền sang ban đầu (Mặc định nếu trống là Monday)
            val thuDuocChon = arguments?.getString("KEY_THU") ?: "Monday"

            // Đóng gói hành lý bắn ngược về cho bảng lịch
            val resultBundle = Bundle()
            resultBundle.putString("TRA_VE_THU", thuDuocChon)
            resultBundle.putString("TRA_VE_GIO", time) // Trả về chuỗi giờ chọn
            resultBundle.putString("TRA_VE_NOI_DUNG", "$topic: $content") //

            // Phát tín hiệu "LUU_SU_KIEN" lên hệ thống
            parentFragmentManager.setFragmentResult("LUU_SU_KIEN", resultBundle)

            Toast.makeText(requireContext(), "Đã lưu sự kiện!", Toast.LENGTH_SHORT).show()
            dismiss() // Đóng popup
        }
    }
}