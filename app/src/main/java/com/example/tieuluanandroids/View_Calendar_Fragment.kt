package com.example.tieuluanandroids

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment


class View_Calendar_Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Nạp file XML man_hinh_chinh.xml
        return inflater.inflate(R.layout.fragment_calendar_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tự động quét 24 tiếng x 7 ngày để gắn sự kiện mở Popup
        duyetQuaCacCotNgay(view)

        val scrollHeader = view.findViewById<HorizontalScrollView>(R.id.scroll_header)
        val scrollBody = view.findViewById<HorizontalScrollView>(R.id.scroll_body)
        scrollBody.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            scrollHeader.scrollTo(scrollX, 0)
        }

        parentFragmentManager.setFragmentResultListener("LUU_SU_KIEN", viewLifecycleOwner) { _, bundle ->
            val thu = bundle.getString("TRA_VE_THU") ?: ""
            val gioBatDau = bundle.getString("TRA_VE_TIME_START") ?: ""
            val gioKetThuc = bundle.getString("TRA_VE_TIME_END") ?: ""
            val noiDung = bundle.getString("TRA_VE_NOI_DUNG") ?: ""

            val thuClean = thu.lowercase()
            var gioClean = gioBatDau.replace(" ", "")
            if (gioClean.contains(":")) {
                try {
                    val parts = gioClean.split(":")
                    val hour = parts[0].toInt()
                    val amPm = if (hour < 12) "AM" else "PM"
                    val hour12 = if (hour % 12 == 0) 12 else hour % 12

                    gioClean = "$hour12$amPm"
                } catch (e: Exception) {
                    gioClean = gioClean.replace(":", "")
                }
            }

            val idString = "col_${thuClean}_$gioClean"

            val cellId = resources.getIdentifier(idString, "id", requireContext().packageName)

            if (cellId != 0) {
                val cell = view.findViewById<FrameLayout>(cellId)
                if (cell != null) {
                    addEventToCell(cell, noiDung, thu, gioBatDau, gioKetThuc)
                }
            }
        }
    }


    /**
     * Hàm duyệt qua từng cột ngày (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
     * và từng ô thời gian trong mỗi cột
     */
    private fun duyetQuaCacCotNgay(view: View) {
        val weeklyGrid = view.findViewById<LinearLayout>(R.id.layout_weekly_grid)

        if (weeklyGrid != null) {
            val columns = listOf(
                R.id.col_monday to "Monday",
                R.id.col_tuesday to "Tuesday",
                R.id.col_wednesday to "Wednesday",
                R.id.col_thursday to "Thursday",
                R.id.col_friday to "Friday",
                R.id.col_saturday to "Saturday",
                R.id.col_sunday to "Sunday"
            )

            val timeAxis = view.findViewById<LinearLayout>(R.id.layout_time_axis)
            val timeTexts = mutableListOf<String>()

            if (timeAxis != null) {
                for (i in 0 until timeAxis.childCount) {
                    val child = timeAxis.getChildAt(i)
                    if (child is TextView) {
                        timeTexts.add(child.text.toString())
                    }
                }
            }

            // Duyệt qua từng cột ngày
            for ((colId, dayName) in columns) {
                val column = view.findViewById<LinearLayout>(colId)
                if (column != null) {
                    // Duyệt qua từng ô (FrameLayout) trong cột
                    for (i in 0 until column.childCount) {
                        val cell = column.getChildAt(i)
                        if (cell is FrameLayout) {
                            // Lấy mốc thời gian tương ứng
                            val timeSlot = if (i < timeTexts.size) timeTexts[i] else "Unknown"

                            // Gắn sự kiện click cho từng ô
                            cell.setOnClickListener {
                                // 1. Bọc hành lý gồm Ngày và Giờ của ô vừa bấm
                                val bundle = Bundle()
                                bundle.putString("KEY_THU", dayName) // Hợp lệ vì nằm trong vòng lặp chứa dayName!
                                bundle.putString("KEY_GIO", timeSlot) // Hợp lệ vì nằm trong vòng lặp chứa timeSlot!

                                // Kiểm tra xem ô này đã có dữ liệu chưa (để hiển thị lại trên Popup)
                                val data = cell.tag as? Bundle
                                if (data != null) {
                                    bundle.putString("EDIT_NOI_DUNG", data.getString("GOC_NOI_DUNG"))
                                    bundle.putString("EDIT_BAT_DAU", data.getString("GOC_BAT_DAU"))
                                    bundle.putString("EDIT_KET_THUC", data.getString("GOC_KET_THUC"))
                                }

                                // 2. Tạo Popup và buộc hành lý vào
                                val popup = PopupFragment()
                                popup.arguments = bundle //

                                // 3. Bật Popup bung từ dưới màn hình lên
                                popup.show(parentFragmentManager, popup.tag)
                            }
                        }
                    }
                }
            }

            // In ra log thông báo đã duyệt xong
            println("Đã duyệt xong các cột ngày: ${columns.size} cột, mỗi cột ${timeTexts.size} ô")
        } else {
            println("Không tìm thấy layout_weekly_grid trong man_hinh_chinh.xml")
        }
    }

    /**
     * Hàm thêm sự kiện vào một ô cụ thể
     * @param cell Ô cần thêm sự kiện (FrameLayout)
     * @param noiDungGoc Nội dung gốc (Topic: Content)
     * @param day Ngày trong tuần
     * @param timeStart Thời gian bắt đầu
     * @param timeEnd Thời gian kết thúc
     */
    fun addEventToCell(cell: FrameLayout, noiDungGoc: String, day: String, timeStart: String, timeEnd: String) {
        // Xóa view cũ nếu có
        cell.removeAllViews()

        // Lưu thông tin vào tag để khi click lại có thể truyền sang Popup
        val info = Bundle().apply {
            putString("GOC_NOI_DUNG", noiDungGoc)
            putString("GOC_BAT_DAU", timeStart)
            putString("GOC_KET_THUC", timeEnd)
        }
        cell.tag = info

        // Tạo TextView để hiển thị sự kiện
        val eventView = TextView(cell.context).apply {
            text = "$noiDungGoc\n($timeStart - $timeEnd)"
            textSize = 16f
            setPadding(8, 8, 8, 8)

            val shape = android.graphics.drawable.GradientDrawable()
            shape.setColor(Color.parseColor("#2196F3")) // Màu nền xanh
            shape.setStroke(3, Color.WHITE)             // Độ dày viền 3px, màu Trắng
            shape.cornerRadius = 8f                     // Bo góc một chút cho đẹp
            
            background = shape
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Thêm vào FrameLayout
        cell.addView(eventView)
    }

    /**
     * Hàm phụ: Lấy danh sách tất cả các ô trong một cột ngày cụ thể
     * @param view Root view
     * @param columnId Id của cột (VD: R.id.col_monday)
     * @return Danh sách các FrameLayout trong cột đó
     */
    fun getCellsInColumn(view: View, columnId: Int): List<FrameLayout> {
        val cells = mutableListOf<FrameLayout>()
        val column = view.findViewById<LinearLayout>(columnId)
        if (column != null) {
            for (i in 0 until column.childCount) {
                val child = column.getChildAt(i)
                if (child is FrameLayout) {
                    cells.add(child)
                }
            }
        }
        return cells
    }

    /**
     * Hàm phụ: Lấy tất cả mốc thời gian
     * @param view Root view
     * @return Danh sách các mốc thời gian (1 AM, 2 AM, ...)
     */
    fun getTimeSlots(view: View): List<String> {
        val timeSlots = mutableListOf<String>()
        val timeAxis = view.findViewById<LinearLayout>(R.id.layout_time_axis)
        if (timeAxis != null) {
            for (i in 0 until timeAxis.childCount) {
                val child = timeAxis.getChildAt(i)
                if (child is TextView) {
                    timeSlots.add(child.text.toString())
                }
            }
        }
        return timeSlots
    }


}