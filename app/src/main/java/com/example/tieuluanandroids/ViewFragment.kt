package com.example.tieuluanandroids

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment


class ViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Nạp file XML man_hinh_chinh.xml (giao diện lịch của bạn) lên
        return inflater.inflate(R.layout.man_hinh_chinh, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Sau khi giao diện được tạo, gọi hàm duyệt qua từng cột ngày
        duyetQuaCacCotNgay(view)
    }

    /**
     * Hàm duyệt qua từng cột ngày (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
     * và từng ô thời gian trong mỗi cột
     */
    private fun duyetQuaCacCotNgay(view: View) {
        // Lấy layout chứa các cột ngày (layout_weekly_grid)
        val weeklyGrid = view.findViewById<LinearLayout>(R.id.layout_weekly_grid)

        if (weeklyGrid != null) {
            // Danh sách các cột ngày theo thứ tự
            val columns = listOf(
                R.id.col_monday to "Monday",
                R.id.col_tuesday to "Tuesday",
                R.id.col_wednesday to "Wednesday",
                R.id.col_thursday to "Thursday",
                R.id.col_friday to "Friday",
                R.id.col_saturday to "Saturday",
                R.id.col_sunday to "Sunday"
            )

            // Lấy cột thời gian để biết mốc giờ
            val timeAxis = view.findViewById<LinearLayout>(R.id.layout_time_axis)
            val timeTexts = mutableListOf<String>()

            // Lấy tất cả mốc thời gian từ cột time axis
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
                                Toast.makeText(context, "$dayName - $timeSlot", Toast.LENGTH_SHORT).show()
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
     * @param eventName Tên sự kiện
     * @param day Ngày trong tuần
     * @param time Thời gian
     */
    fun addEventToCell(cell: FrameLayout, eventName: String, day: String, time: String) {
        // Xóa view cũ nếu có
        cell.removeAllViews()

        // Tạo TextView để hiển thị sự kiện
        val eventView = TextView(cell.context).apply {
            text = eventName
            textSize = 10f
            setPadding(4, 4, 4, 4)
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        // Thêm vào FrameLayout
        cell.addView(eventView)

        // Gắn sự kiện click cho ô
        cell.setOnClickListener {
            Toast.makeText(cell.context, "$day - $time: $eventName", Toast.LENGTH_SHORT).show()
        }
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