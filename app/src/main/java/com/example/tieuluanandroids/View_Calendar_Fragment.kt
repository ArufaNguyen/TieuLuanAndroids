package com.example.tieuluanandroids

import android.content.Context
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
        // Nạp file XML fragment_calendar_view.xml
        return inflater.inflate(R.layout.fragment_calendar_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tự động quét 24 tiếng x 7 ngày để gắn sự kiện mở Popup
        duyetQuaCacCotNgay(view)

        // Tính năng kéo trượt đồng bộ Thứ và Ô của Thắng
        val scrollHeader = view.findViewById<HorizontalScrollView>(R.id.scroll_header)
        val scrollBody = view.findViewById<HorizontalScrollView>(R.id.scroll_body)
        scrollBody.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            scrollHeader.scrollTo(scrollX, 0)
        }
        parentFragmentManager.setFragmentResultListener("LUU_SU_KIEN", viewLifecycleOwner) { _, bundle ->
            val thu = bundle.getString("TRA_VE_THU") ?: ""         // Ví dụ: "Monday"
            val gio = bundle.getString("TRA_VE_GIO") ?: ""         // Ví dụ: "1 AM" hoặc "12 PM"
            val noiDung = bundle.getString("TRA_VE_NOI_DUNG") ?: "" //
            val gioBatDau = bundle.getString("TRA_VE_BAT_DAU") ?: ""
            val gioKetThuc = bundle.getString("TRA_VE_KET_THUC") ?: ""
            // Gọi hàm thông minh để check trùng lịch và xếp chồng thay vì add trực tiếp
            xuLySuKienThongMinh(view, thu, gioBatDau, gioKetThuc, noiDung)
        }
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
                                val bundle = Bundle()
                                bundle.putString("KEY_THU", dayName)
                                bundle.putString("KEY_GIO", timeSlot)

                                // Kiểm tra xem ô này đã có dữ liệu chưa (để hiển thị lại trên Popup)
                                val data = cell.tag as? Bundle
                                if (data != null) {
                                    bundle.putString("EDIT_NOI_DUNG", data.getString("GOC_NOI_DUNG"))
                                    bundle.putString("EDIT_BAT_DAU", data.getString("GOC_BAT_DAU"))
                                    bundle.putString("EDIT_KET_THUC", data.getString("GOC_KET_THUC"))
                                }

                                val popup = PopupFragment()
                                popup.arguments = bundle
                                popup.show(parentFragmentManager, popup.tag)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Hàm thêm sự kiện vào một ô cụ thể (Trường hợp ô trống hoàn toàn)
     */
    fun addEventToCell(cell: FrameLayout, noiDungGoc: String, day: String, timeStart: String, timeEnd: String) {
        cell.removeAllViews()

        // Gắn Bundle dữ liệu gốc vào tag dữ liệu ẩn để đồng bộ cấu trúc
        val info = Bundle().apply {
            putString("GOC_NOI_DUNG", noiDungGoc)
            putString("GOC_BAT_DAU", timeStart)
            putString("GOC_KET_THUC", timeEnd)
        }

        // Tạo TextView để hiển thị sự kiện
        val eventView = TextView(cell.context).apply {
            text = "$noiDungGoc\n($timeStart - $timeEnd)"
            textSize = 14f // Đồng bộ kích thước font chữ 14f
            setPadding(8, 8, 8, 8)

            val shape = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#2196F3")) // Màu nền xanh
                setStroke(3, Color.WHITE)             // Độ dày viền 3px, màu Trắng
                cornerRadius = 8f                     // Bo góc 8f
            }
            background = shape
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)

            // Gắn tag dữ liệu ẩn vào chính TextView này
            tag = info
        }

        cell.addView(eventView)

        // Gắn sự kiện click cho ô
    }

    fun xuLySuKienThongMinh(
        view: View,
        thu: String,
        gioBatDau: String,
        gioKetThuc: String,
        noiDung: String
    ) {
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
        val cellId = view.resources.getIdentifier(idString, "id", view.context.packageName)

        if (cellId != 0) {
            val cell = view.findViewById<FrameLayout>(cellId) ?: return

            // TRƯỜNG HỢP 1: Ô trống hoàn toàn -> Thêm ngay sự kiện đầu tiên
            if (cell.childCount == 0) {
                addEventToCell(cell, noiDung, thu, gioBatDau, gioKetThuc)
                Toast.makeText(view.context, "Lưu sự kiện thành công!", Toast.LENGTH_SHORT).show()
                return
            }
            android.app.AlertDialog.Builder(view.context)
                .setTitle("Xung đột lịch trình.")
                .setMessage("Khung giờ này đang có hoạt động, bạn có muốn thêm hoạt động này không?")
                .setPositiveButton("Yes") { _, _ ->
                    val danhSachSuKien = mutableListOf<Bundle>()

                    val childView = cell.getChildAt(0)
                    if (childView is LinearLayout) {
                        for (i in 0 until childView.childCount) {
                            val tv = childView.getChildAt(i) as? TextView
                            val info = tv?.tag as? Bundle
                            if (info != null) danhSachSuKien.add(info)
                        }
                    } else if (childView is TextView) {
                        val info = childView.tag as? Bundle
                        if (info != null) danhSachSuKien.add(info)
                    }

                    // Đóng gói thông tin sự kiện mới nhét vào danh sách tổng
                    val bundleMoi = Bundle().apply {
                        putString("GOC_NOI_DUNG", noiDung)
                        putString("GOC_BAT_DAU", gioBatDau)
                        putString("GOC_KET_THUC", gioKetThuc)
                    }
                    danhSachSuKien.add(bundleMoi)

                    danhSachSuKien.sortBy { it.getString("GOC_BAT_DAU") ?: "00:00" }

                    cell.removeAllViews()

                    // Tạo LinearLayout dọc làm hộp chứa đống xếp chồng chia đều không gian
                    val linearLayoutXepChong = LinearLayout(cell.context).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }

                    for (info in danhSachSuKien) {
                        val name = info.getString("GOC_NOI_DUNG") ?: ""
                        val start = info.getString("GOC_BAT_DAU") ?: ""
                        val end = info.getString("GOC_KET_THUC") ?: ""

                        val tv = taoEventTextView(cell.context, name, start, end)
                        tv.tag = info // Gắn tag để bấm click xem tiếp thông tin

                        // Gắn click cho từng cục sự kiện nhỏ trong đống xếp chồng để mở Popup sửa đổi
                        tv.setOnClickListener {
                            val bundle = Bundle().apply {
                                putString("KEY_THU", thu)
                                putString("KEY_GIO", start)
                                putString("EDIT_NOI_DUNG", name)
                                putString("EDIT_BAT_DAU", start)
                                putString("EDIT_KET_THUC", end)
                            }
                            val popup = PopupFragment()
                            popup.arguments = bundle
                            popup.show(parentFragmentManager, popup.tag)
                        }
                        linearLayoutXepChong.addView(tv)
                    }
                    cell.addView(linearLayoutXepChong)
                    Toast.makeText(view.context, "Đã xếp chồng ${danhSachSuKien.size} sự kiện!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    /**
     * Hàm phụ: Tạo nhanh giao diện TextView con chia đều chiều cao ô lịch (Weight = 1f)
     */
    private fun taoEventTextView(
        context: Context,
        name: String,
        start: String,
        end: String
    ): TextView {
        return TextView(context).apply {
            text = "$name\n($start - $end)"
            textSize = 12f // Hạ font xuống chút để xếp chồng không bị tràn khung
            setPadding(8, 4, 8, 4)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)

            val shape = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#2196F3"))
                setStroke(2, Color.WHITE)
                cornerRadius = 8f
            }
            background = shape

            // Sử dụng chiều cao bằng 0 và trọng số weight = 1f để chia đều diện tích ô
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(0, 2, 0, 2)
            }
        }
    }

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