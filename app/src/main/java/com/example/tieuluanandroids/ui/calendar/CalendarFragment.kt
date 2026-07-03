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

        duyetQuaCacCotNgay(view)

        val scrollHeader = view.findViewById<HorizontalScrollView>(R.id.scroll_header)
        val scrollBody = view.findViewById<HorizontalScrollView>(R.id.scroll_body)
        scrollBody.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            scrollHeader.scrollTo(scrollX, 0)
        }
        parentFragmentManager.setFragmentResultListener("LUU_SU_KIEN", viewLifecycleOwner) { _, bundle ->
            val hanhDong = bundle.getString("HANH_DONG") ?: "SAVE"
            val eventId = bundle.getString("XOA_EVENT_ID").orEmpty()

            if (hanhDong == "DELETE") {
                if (eventId.isNotBlank()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        when (val result = data.deleteEvent(eventId)) {
                            is AppResult.Success -> {
                                Toast.makeText(requireContext(), "Đã xóa sự kiện khỏi cơ sở dữ liệu!", Toast.LENGTH_SHORT).show()
                            }
                            is AppResult.Error -> {
                                Toast.makeText(requireContext(), "Lỗi: ${result.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Lỗi: Không tìm thấy ID sự kiện để xóa!", Toast.LENGTH_SHORT).show()
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
            val hanhDong = bundle.getString("HANH_DONG") ?: "SAVE"
            if (hanhDong == "DELETE") return@setFragmentResultListener

            val dayName = bundle.getString("TRA_VE_THU").orEmpty()
            val title = bundle.getString("TRA_VE_NOI_DUNG").orEmpty()
            
            val startTime = bundle.getString("TRA_VE_TIME_START")
                ?: bundle.getString("TRA_VE_GIO")
                ?: "08:00"
            val endTime = bundle.getString("TRA_VE_TIME_END") ?: defaultEndTime(startTime)

            if (title.isBlank()) {
                Toast.makeText(requireContext(), "Nội dung sự kiện đang trống", Toast.LENGTH_SHORT).show()
                return@setFragmentResultListener
            }

            val eventDate = dateForWeekday(dayName)
            val start = combineDateAndTime(eventDate, startTime)
            val end = combineDateAndTime(eventDate, endTime).apply {
                if (!after(start)) add(Calendar.DAY_OF_MONTH, 1)
            }

            val thucHienLuuVaoDatabase: () -> Unit = {
                viewLifecycleOwner.lifecycleScope.launch {
                    when (val result = data.createEvent(
                        CreateEventInput(
                            title = title,
                            description = null,
                            startTime = backendDateTime(start),
                            endTime = backendDateTime(end)
                        )
                    )) {
                        is AppResult.Success -> {
                            Toast.makeText(requireContext(), "Đã lưu vào cơ sở dữ liệu", Toast.LENGTH_SHORT).show()
                        }
                        is AppResult.Error -> {
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            xuLySuKienThongMinh(rootView, dayName, startTime, endTime, title, thucHienLuuVaoDatabase)
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
                "${block.event.title}\n${formatEventTimeRange(block)}".trim()
            } else {
                ""
            }
            textSize = 14f
            maxLines = 2
            setPadding(dp(12), dp(3), dp(4), dp(4))
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
                    if (child is TextView) timeTexts.add(child.text.toString())
                }
            }

            for ((colId, dayName) in columns) {
                val column = view.findViewById<LinearLayout>(colId)
                if (column != null) {
                    for (i in 0 until column.childCount) {
                        val cell = column.getChildAt(i)
                        if (cell is FrameLayout) {
                            val timeSlot = if (i < timeTexts.size) timeTexts[i] else "Unknown"

                            cell.setOnClickListener {
                                val bundle = Bundle()
                                bundle.putString("KEY_THU", dayName)
                                bundle.putString("KEY_GIO", timeSlot)

                                val eventDateKey = dateKey(dateForWeekday(dayName))
                                val matchedEvent = currentEvents.find { event ->
                                    val startCal = parseBackendTime(event.startTime)
                                    if (startCal != null) {
                                        val startDayKey = dateKey(startCal)
                                        val slotIndex = slotIndexForMinute(startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE))
                                        startDayKey == eventDateKey && slotIndex == i
                                    } else {
                                        false
                                    }
                                }
                                if (matchedEvent != null) {
                                    bundle.putString("KEY_EVENT_ID", matchedEvent.localId)
                                    bundle.putString("KEY_NOI_DUNG", matchedEvent.title)
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
     * Hàm thêm sự kiện vào một ô cụ thể
     * @param cell Ô cần thêm sự kiện (FrameLayout)
     * @param eventName Tên sự kiện
     * @param day Ngày trong tuần
     * @param time Thời gian
     */
    fun addEventToCell(cell: FrameLayout, noiDungGoc: String, day: String, timeStart: String, timeEnd: String) {
        cell.removeAllViews()

        val info = Bundle().apply {
            putString("GOC_NOI_DUNG", noiDungGoc)
            putString("GOC_BAT_DAU", timeStart)
            putString("GOC_KET_THUC", timeEnd)
        }

        val eventView = TextView(cell.context).apply {
            text = "$noiDungGoc\n($timeStart - $timeEnd)"
            textSize = 14f
            setPadding(8, 8, 8, 8)

            val shape = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#2196F3")) // Màu nền xanh rực rỡ
                setStroke(3, Color.WHITE)             // Độ dày viền 3px, màu Trắng
                cornerRadius = 8f                     // Bo góc mượt mà
            }
            background = shape
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)

            tag = info
        }

        cell.addView(eventView)
        cell.tag = info
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
    fun xuLySuKienThongMinh(
        view: View,
        thu: String,
        gioBatDau: String,
        gioKetThuc: String,
        noiDung: String,
        onConfirmSave: () -> Unit
    ) {
        val thuLower = thu.lowercase()
        val thuClean = when {
            thuLower.contains("monday") || thuLower.contains("thứ 2") || thuLower.contains("t2") || thuLower.contains("mon") -> "monday"
            thuLower.contains("tuesday") || thuLower.contains("thứ 3") || thuLower.contains("t3") || thuLower.contains("tue") -> "tuesday"
            thuLower.contains("wednesday") || thuLower.contains("thứ 4") || thuLower.contains("t4") || thuLower.contains("wed") -> "wednesday"
            thuLower.contains("thursday") || thuLower.contains("thứ 5") || thuLower.contains("t5") || thuLower.contains("thu") -> "thursday"
            thuLower.contains("friday") || thuLower.contains("thứ 6") || thuLower.contains("t6") || thuLower.contains("fri") -> "friday"
            thuLower.contains("saturday") || thuLower.contains("thứ 7") || thuLower.contains("t7") || thuLower.contains("sat") -> "saturday"
            thuLower.contains("sunday") || thuLower.contains("chủ nhật") || thuLower.contains("cn") || thuLower.contains("sun") -> "sunday"
            else -> "monday"
        }

        fun getIdFromHour(h: Int): String {
            val amPm = if (h < 12 || h == 24) "AM" else "PM"
            val h12 = when {
                h == 0 || h == 24 -> 12
                h > 12 -> h - 12
                else -> h
            }
            return "$h12$amPm"
        }

        try {
            val startH = gioBatDau.split(":")[0].toInt()
            val endH = gioKetThuc.split(":")[0].toInt()

            var coOTrung = false
            for (h in startH until endH) {
                val idString = "col_${thuClean}_${getIdFromHour(h)}"
                val cellId = view.resources.getIdentifier(idString, "id", view.context.packageName)
                if (cellId != 0 && (view.findViewById<FrameLayout>(cellId)?.childCount ?: 0) > 0) {
                    coOTrung = true
                    break
                }
            }

            fun thucHienToMau() {
                for (h in startH until endH) {
                    val idString = "col_${thuClean}_${getIdFromHour(h)}"
                    val cellId = view.resources.getIdentifier(idString, "id", view.context.packageName)
                    if (cellId != 0) {
                        val cell = view.findViewById<FrameLayout>(cellId) ?: continue
                        if (cell.childCount == 0) {
                            addEventToCell(cell, noiDung, thu, gioBatDau, gioKetThuc)
                        } else {
                            val danhSachSuKien = mutableListOf<Bundle>()
                            val childView = cell.getChildAt(0)
                            
                            if (childView is LinearLayout) {
                                for (i in 0 until childView.childCount) {
                                    (childView.getChildAt(i).tag as? Bundle)?.let { danhSachSuKien.add(it) }
                                }
                            } else if (childView is TextView) {
                                (childView.tag as? Bundle)?.let { danhSachSuKien.add(it) }
                            }

                            val bundleMoi = Bundle().apply {
                                putString("GOC_NOI_DUNG", noiDung)
                                putString("GOC_BAT_DAU", gioBatDau)
                                putString("GOC_KET_THUC", gioKetThuc)
                            }
                            danhSachSuKien.add(bundleMoi)
                            
                            cell.removeAllViews()
                            val container = LinearLayout(cell.context).apply {
                                orientation = LinearLayout.VERTICAL
                                layoutParams = FrameLayout.LayoutParams(-1, -1)
                            }
                            danhSachSuKien.distinctBy { it.getString("GOC_NOI_DUNG") }.forEach { info ->
                                container.addView(taoEventTextView(cell.context, info.getString("GOC_NOI_DUNG") ?: "", info.getString("GOC_BAT_DAU") ?: "", info.getString("GOC_KET_THUC") ?: "").apply { tag = info })
                            }
                            cell.addView(container)
                        }
                    }
                }
                onConfirmSave()
            }

            if (coOTrung) {
                android.app.AlertDialog.Builder(view.context)
                    .setTitle("Xung đột lịch trình")
                    .setMessage("Khung giờ này đã có hoạt động. Bạn có muốn xếp chồng thêm hoạt động này không?")
                    .setPositiveButton("Có") { _, _ -> thucHienToMau() }
                    .setNegativeButton("Không", null)
                    .show()
            } else {
                thucHienToMau()
            }

        } catch (e: Exception) {}
    }

    private fun taoEventTextView(
        context: android.content.Context,
        name: String,
        start: String,
        end: String
    ): TextView {
        return TextView(context).apply {
            text = "$name\n($start - $end)"
            textSize = 12f
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

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(0, 2, 0, 2)
            }
        }
    }

}
