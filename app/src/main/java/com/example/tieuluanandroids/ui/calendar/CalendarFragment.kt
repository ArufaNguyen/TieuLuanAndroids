package com.example.tieuluanandroids.ui.calendar

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tieuluanandroids.PopupFragment
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.AppResult
import com.example.tieuluanandroids.model.CreateEventInput
import com.example.tieuluanandroids.model.Event
import com.example.tieuluanandroids.model.service.SmartCalendarData
import com.example.tieuluanandroids.ui.settings.CalendarSettings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private val selectedDate: Calendar = Calendar.getInstance().apply {
        resetTime()
    }
    private val selectedWeekStart: Calendar = Calendar.getInstance().apply {
        resetTime()
        time = selectedDate.time
        moveToWeekStart()
    }
    private lateinit var monthTitle: TextView
    private lateinit var dayHeaders: List<TextView>
    private lateinit var tagFilterSpinner: Spinner
    private lateinit var rootView: View
    private var visibleDates: List<Calendar> = emptyList()
    private var currentEvents: List<Event> = emptyList()
    private var currentTagNames: List<String> = emptyList()
    private var selectedTagName: String? = null
    private var updatingTagFilter = false
    private var MODE: String = CalendarSettings.MODE_LINE
    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Nạp file XML man_hinh_chinh.xml (giao diện lịch của bạn) lên
        MODE = CalendarSettings.getWeeklyCalendarMode(requireContext())
        val layoutId = when (MODE) {
            CalendarSettings.MODE_BOX -> R.layout.fragment_calendar_view
            else -> R.layout.fragment_calendar_view_v2
        }
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view

        if (MODE == CalendarSettings.MODE_BOX) {
            setupBoxMode(view)
            return
        }

        monthTitle = view.findViewById(R.id.text_calendar_month)
        tagFilterSpinner = view.findViewById(R.id.spinner_tag_filter)
        dayHeaders = listOf(
            view.findViewById(R.id.text_monday_header),
            view.findViewById(R.id.text_tuesday_header),
            view.findViewById(R.id.text_wednesday_header),
            view.findViewById(R.id.text_thursday_header),
            view.findViewById(R.id.text_friday_header),
            view.findViewById(R.id.text_saturday_header),
            view.findViewById(R.id.text_sunday_header)
        )
        view.findViewById<Button>(R.id.button_previous_month).setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, -7)
            syncSelectedWeekStart()
            updateMonthHeader()
            renderEvents()
        }
        view.findViewById<Button>(R.id.button_next_month).setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, 7)
            syncSelectedWeekStart()
            updateMonthHeader()
            renderEvents()
        }
        view.findViewById<Button>(R.id.button_today).setOnClickListener {
            selectedDate.time = Calendar.getInstance().time
            selectedDate.resetTime()
            syncSelectedWeekStart()
            updateMonthHeader()
            renderEvents()
        }
        monthTitle.setOnClickListener {
            showDatePicker()
        }
        updateMonthHeader()
        setupTagFilter()

        // Tự động quét 24 tiếng x 7 ngày để gắn sự kiện mở Popup
        duyetQuaCacCotNgay(view)

        // Tính năng kéo trượt đồng bộ Thứ và Ô của Thắng
        val scrollHeader = view.findViewById<HorizontalScrollView>(R.id.scroll_header)
        val scrollBody = view.findViewById<HorizontalScrollView>(R.id.scroll_body)
        scrollBody.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            scrollHeader.scrollTo(scrollX, 0)
        }

        // 🔥 THÊM ĐOẠN NÀY: Bộ bắt sóng tự động vẽ chữ lên ô lịch
        parentFragmentManager.setFragmentResultListener("LUU_SU_KIEN", viewLifecycleOwner) { _, bundle ->
            val thu = bundle.getString("TRA_VE_THU") ?: ""         // Ví dụ: "Monday"
            val gio = bundle.getString("TRA_VE_GIO") ?: ""         // Ví dụ: "1 AM" hoặc "12 PM"
            val noiDung = bundle.getString("TRA_VE_NOI_DUNG") ?: "" //

            // Chuẩn hóa chuỗi chữ để ráp thành ID tương thích XML (Ví dụ: "Monday" -> "monday", "1 AM" -> "1AM")
            val thuClean = thu.lowercase()
            val gioClean = gio.replace(" ", "") // Xóa khoảng trắng để tạo chữ "1AM", "12PM"

            // Tự động ghép chuỗi thành ID, ví dụ: "col_monday_1AM"
            val idString = "col_${thuClean}_$gioClean"

            // Biến chuỗi chữ thành ID hệ thống thật sự R.id
            val cellId = resources.getIdentifier(idString, "id", requireContext().packageName)

            // Nếu dò trúng ô FrameLayout đó trên giao diện, nạp chữ màu xanh lá vào luôn!
            if (cellId != 0) {
                val cell = view.findViewById<FrameLayout>(cellId)
                if (cell != null) {
                    // Gọi hàm vẽ cục màu xanh lá cây hiển thị nội dung của bạn
                    addEventToCell(cell, noiDung, thu, gio)
                }
            }
        }
        setupPopupResultListener()
        observeEvents()
        observeTags()
        refreshEventsOnOpen()
    }

    private fun setupBoxMode(view: View) {
        updateBoxModeHeader(view)
        duyetQuaCacCotNgay(view)
        val scrollHeader = view.findViewById<HorizontalScrollView>(R.id.scroll_header)
        val scrollBody = view.findViewById<HorizontalScrollView>(R.id.scroll_body)
        scrollBody?.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            scrollHeader?.scrollTo(scrollX, 0)
        }
        setupPopupResultListener()
        observeEvents()
        refreshEventsOnOpen()
    }

    private fun updateBoxModeHeader(view: View) {
        val firstVisibleDay = selectedWeekStart.clone() as Calendar
        val dayFormat = SimpleDateFormat("EEE dd/MM", Locale.getDefault())
        visibleDates = (0..6).map { index ->
            (firstVisibleDay.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, index)
            }
        }
        val header = view.findViewById<LinearLayout>(R.id.layout_sticky_header)
        visibleDates.forEachIndexed { index, day ->
            (header?.getChildAt(index) as? TextView)?.text = dayFormat.format(day.time)
        }
    }

    private fun setupPopupResultListener() {
        parentFragmentManager.setFragmentResultListener("LUU_SU_KIEN", viewLifecycleOwner) { _, bundle ->
            val dayName = bundle.getString("TRA_VE_THU").orEmpty()
            val title = bundle.getString("TRA_VE_NOI_DUNG").orEmpty()
            val startTime = bundle.getString("TRA_VE_GIO_BAT_DAU")
                ?: bundle.getString("TRA_VE_GIO")
                ?: "08:00"
            val endTime = bundle.getString("TRA_VE_GIO_KET_THUC") ?: defaultEndTime(startTime)

            if (title.isBlank()) {
                Toast.makeText(requireContext(), "Noi dung su kien dang trong", Toast.LENGTH_SHORT).show()
                return@setFragmentResultListener
            }

            val eventDate = dateForWeekday(dayName)
            val start = combineDateAndTime(eventDate, startTime)
            val end = combineDateAndTime(eventDate, endTime).apply {
                if (!after(start)) add(Calendar.DAY_OF_MONTH, 1)
            }

            viewLifecycleOwner.lifecycleScope.launch {
                when (
                    val result = data.createEvent(
                        CreateEventInput(
                            title = title,
                            description = null,
                            startTime = backendDateTime(start),
                            endTime = backendDateTime(end)
                        )
                    )
                ) {
                    is AppResult.Success -> {
                        Toast.makeText(requireContext(), "Da luu su kien", Toast.LENGTH_SHORT).show()
                    }

                    is AppResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun dateForWeekday(dayName: String): Calendar {
        val index = when (dayName.lowercase(Locale.US)) {
            "monday", "mon" -> 0
            "tuesday", "tue" -> 1
            "wednesday", "wed" -> 2
            "thursday", "thu" -> 3
            "friday", "fri" -> 4
            "saturday", "sat" -> 5
            "sunday", "sun" -> 6
            else -> 0
        }
        return (visibleDates.getOrNull(index) ?: selectedDate).clone() as Calendar
    }

    private fun combineDateAndTime(date: Calendar, time: String): Calendar {
        val parts = time.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.take(2)?.toIntOrNull() ?: 0
        return (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun defaultEndTime(startTime: String): String {
        val start = combineDateAndTime(selectedDate, startTime)
        start.add(Calendar.HOUR_OF_DAY, 1)
        return SimpleDateFormat("HH:mm", Locale.US).format(start.time)
    }

    private fun backendDateTime(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(calendar.time)


    /**
     * Hàm duyệt qua từng cột ngày (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
     * và từng ô thời gian trong mỗi cột
     */
    private fun updateMonthHeader() {
        val firstVisibleDay = selectedWeekStart.clone() as Calendar
        monthTitle.text = dateKey(selectedDate)
        val dayFormat = SimpleDateFormat("EEE dd/MM", Locale.getDefault())
        visibleDates = dayHeaders.mapIndexed { index, header ->
            val day = (firstVisibleDay.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, index)
            }
            header.text = dayFormat.format(day.time)
            day
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                selectedDate.resetTime()
                syncSelectedWeekStart()
                updateMonthHeader()
                renderEvents()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun syncSelectedWeekStart() {
        selectedWeekStart.time = selectedDate.time
        selectedWeekStart.resetTime()
        selectedWeekStart.moveToWeekStart()
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                data.observeEvents().collect { events ->
                    currentEvents = events
                    updateTagFilterOptions()
                    renderEvents()
                }
            }
        }
    }

    private fun observeTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                data.observeTags().collect { tags ->
                    currentTagNames = tags.map { it.name }
                    updateTagFilterOptions()
                }
            }
        }
    }

    private fun setupTagFilter() {
        tagFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (updatingTagFilter) return
                selectedTagName = parent?.getItemAtPosition(position)?.toString()
                    ?.takeIf { it != ALL_TAGS_LABEL }
                renderEvents()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedTagName = null
                renderEvents()
            }
        }
        updateTagFilterOptions()
    }

    private fun updateTagFilterOptions() {
        if (!::tagFilterSpinner.isInitialized) return
        val names = (currentTagNames + currentEvents.map { it.tagName })
            .map(String::trim)
            .filter { it.isNotBlank() && it != "-" }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        if (selectedTagName != null && names.none { it.equals(selectedTagName, ignoreCase = true) }) {
            selectedTagName = null
        }
        val options = listOf(ALL_TAGS_LABEL) + names
        updatingTagFilter = true
        tagFilterSpinner.adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            options
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.setTextColor(Color.BLACK)
                return view
            }
        }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        tagFilterSpinner.setSelection(options.indexOf(selectedTagName ?: ALL_TAGS_LABEL).coerceAtLeast(0))
        updatingTagFilter = false
    }

    private fun refreshEventsOnOpen() {
        viewLifecycleOwner.lifecycleScope.launch {
            data.syncNow()
        }
    }

    private fun renderEvents() {
        if (!::rootView.isInitialized || visibleDates.isEmpty()) return
        clearCalendarCells()
        val visibleDateKeys = visibleDates.map(::dateKey)
        val eventsByDay = mutableMapOf<Int, MutableList<CalendarEventBlock>>()
        currentEvents
            .filter { event -> selectedTagName == null || event.tagName.equals(selectedTagName, ignoreCase = true) }
            .forEach { event ->
            val start = parseBackendTime(event.startTime) ?: return@forEach
            val end = parseBackendTime(event.endTime) ?: return@forEach
            if (!end.after(start)) return@forEach
            val dayIndex = visibleDateKeys.indexOf(dateKey(start))
            if (dayIndex < 0) return@forEach
            eventsByDay.getOrPut(dayIndex) { mutableListOf() } += CalendarEventBlock(
                event = event,
                start = start,
                end = end,
                startMinutes = start.get(Calendar.HOUR_OF_DAY) * 60 + start.get(Calendar.MINUTE),
                endMinutes = endMinutesForRender(start, end)
            )
        }
        eventsByDay.forEach { (dayIndex, events) ->
            assignLanes(events).forEach { block ->
                renderEventRange(dayIndex, block)
            }
        }
    }

    private fun renderEventRange(dayIndex: Int, block: CalendarEventBlock) {
        val startMinutes = block.startMinutes
        val endMinutes = block.endMinutes
        val firstSlot = slotIndexForMinute(startMinutes)
        val lastSlot = slotIndexForMinute((endMinutes - 1).coerceAtLeast(startMinutes))

        val slots: Iterable<Int> = if (firstSlot <= lastSlot) {
            firstSlot..lastSlot
        } else {
            (firstSlot..23) + (0..lastSlot)
        }

        for (slotIndex in slots) {
            val cell = cellFor(dayIndex, slotIndex) ?: continue
            val slotStart = minutesForSlot(slotIndex)
            val slotEnd = slotStart + 60
            val segmentStart = startMinutes.coerceAtLeast(slotStart)
            val segmentEnd = endMinutes.coerceAtMost(slotEnd)
            if (segmentEnd <= segmentStart) continue
            addRenderedEventSegmentToCell(
                cell = cell,
                block = block,
                topMinutes = segmentStart - slotStart,
                durationMinutes = segmentEnd - segmentStart,
                showTitle = slotIndex == firstSlot
            )
        }
    }

    private fun endMinutesForRender(start: Calendar, end: Calendar): Int {
        if (dateKey(end) != dateKey(start)) return 24 * 60
        return end.get(Calendar.HOUR_OF_DAY) * 60 + end.get(Calendar.MINUTE)
    }

    private fun assignLanes(events: List<CalendarEventBlock>): List<CalendarEventBlock> {
        val sorted = events.sortedWith(compareBy<CalendarEventBlock> { it.startMinutes }.thenBy { it.endMinutes })
        val result = mutableListOf<CalendarEventBlock>()
        var index = 0
        while (index < sorted.size) {
            val cluster = mutableListOf<CalendarEventBlock>()
            var clusterEnd = sorted[index].endMinutes
            while (index < sorted.size && sorted[index].startMinutes < clusterEnd) {
                val event = sorted[index]
                cluster += event
                clusterEnd = maxOf(clusterEnd, event.endMinutes)
                index++
            }
            result += assignClusterLanes(cluster)
        }
        return result
    }

    private fun assignClusterLanes(cluster: List<CalendarEventBlock>): List<CalendarEventBlock> {
        val laneEnds = mutableListOf<Int>()
        val assigned = cluster.map { block ->
            val lane = laneEnds.indexOfFirst { it <= block.startMinutes }
            val laneIndex = if (lane >= 0) lane else laneEnds.size
            if (lane >= 0) {
                laneEnds[lane] = block.endMinutes
            } else {
                laneEnds += block.endMinutes
            }
            block.copy(laneIndex = laneIndex)
        }
        val laneCount = laneEnds.size.coerceAtLeast(1)
        return assigned.map { it.copy(laneCount = laneCount) }
    }

    private fun clearCalendarCells() {
        WEEK_COLUMN_IDS.forEach { columnId ->
            val column = rootView.findViewById<LinearLayout>(columnId) ?: return@forEach
            for (i in 0 until column.childCount) {
                (column.getChildAt(i) as? FrameLayout)?.removeAllViews()
            }
        }
    }

    private fun cellFor(dayIndex: Int, slotIndex: Int): FrameLayout? {
        val column = rootView.findViewById<LinearLayout>(WEEK_COLUMN_IDS.getOrNull(dayIndex) ?: return null)
            ?: return null
        return column.getChildAt(slotIndex) as? FrameLayout
    }

    private fun addRenderedEventSegmentToCell(
        cell: FrameLayout,
        block: CalendarEventBlock,
        topMinutes: Int,
        durationMinutes: Int,
        showTitle: Boolean
    ) {
        val eventView = TextView(cell.context).apply {
            text = if (showTitle && block.laneCount <= MAX_TEXT_EVENT_LANES) {
                "${formatEventTimeRange(block)} ${block.event.title}".trim()
            } else {
                ""
            }
            textSize = 10f
            maxLines = 2
            setPadding(dp(4), dp(3), dp(4), dp(3))
            setBackgroundColor(tagColor(block.event.tagName))
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }
        val horizontalGap = dp(4)
        val availableWidth = dp(240) - horizontalGap * 2
        val laneWidth = (availableWidth / block.laneCount).coerceAtLeast(dp(42))
        cell.addView(
            eventView,
            FrameLayout.LayoutParams(
                laneWidth - horizontalGap,
                minuteHeight(durationMinutes).coerceAtLeast(dp(22))
            ).apply {
                leftMargin = horizontalGap + block.laneIndex * laneWidth
                rightMargin = horizontalGap
                topMargin = minuteHeight(topMinutes)
            }
        )
    }

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
                                // 1. Bọc hành lý gồm Ngày và Giờ của ô vừa bấm
                                val bundle = Bundle()
                                bundle.putString("KEY_THU", dayName) // Hợp lệ vì nằm trong vòng lặp chứa dayName!
                                bundle.putString("KEY_GIO", timeSlot) // Hợp lệ vì nằm trong vòng lặp chứa timeSlot!

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

    private fun parseBackendTime(value: String): Calendar? {
        val normalized = value.trim().removeSuffix("Z")
        val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm:ss")
        val parsed = patterns.firstNotNullOfOrNull { pattern ->
            runCatching { SimpleDateFormat(pattern, Locale.US).parse(normalized) }.getOrNull()
        } ?: return null
        return Calendar.getInstance().apply {
            time = parsed
        }
    }

    private fun dateKey(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

    private fun formatEventTimeRange(block: CalendarEventBlock): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return "${formatter.format(block.start.time)}-${formatter.format(block.end.time)}"
    }

    private fun slotIndexForMinute(minuteOfDay: Int): Int {
        val hour = (minuteOfDay / 60).coerceIn(0, 23)
        return if (hour == 0) 23 else hour - 1
    }

    private fun minutesForSlot(slotIndex: Int): Int {
        val hour = if (slotIndex == 23) 0 else slotIndex + 1
        return hour * 60
    }

    private fun minuteHeight(minutes: Int): Int =
        (dp(240) * minutes / 60f).toInt()

    private fun tagColor(tagName: String): Int {
        return when (tagName.lowercase(Locale.US)) {
            "study" -> Color.parseColor("#2196F3")
            "work" -> Color.parseColor("#4CAF50")
            "health" -> Color.parseColor("#D93025")
            "personal" -> Color.parseColor("#8E44AD")
            else -> Color.parseColor("#5F6368")
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun Calendar.resetTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.moveToWeekStart() {
        val mondayOffset = (get(Calendar.DAY_OF_WEEK) + 5) % 7
        add(Calendar.DAY_OF_MONTH, -mondayOffset)
    }

    private data class CalendarEventBlock(
        val event: Event,
        val start: Calendar,
        val end: Calendar,
        val startMinutes: Int,
        val endMinutes: Int,
        val laneIndex: Int = 0,
        val laneCount: Int = 1
    )

    companion object {
        private const val ALL_TAGS_LABEL = "All tags"
        private const val MAX_TEXT_EVENT_LANES = 3
        private val WEEK_COLUMN_IDS = listOf(
            R.id.col_monday,
            R.id.col_tuesday,
            R.id.col_wednesday,
            R.id.col_thursday,
            R.id.col_friday,
            R.id.col_saturday,
            R.id.col_sunday
        )
    }

}
