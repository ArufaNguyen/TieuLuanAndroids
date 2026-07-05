package com.example.tieuluanandroids.ui.menu

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.Event
import com.example.tieuluanandroids.model.SessionInfo
import com.example.tieuluanandroids.model.Tag
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MenuFragment : Fragment() {

    private lateinit var buttonLogin: Button
    private lateinit var buttonViewCalendar2: Button
    private lateinit var buttonViewCalendar: Button
    private lateinit var buttonSession: Button
    private lateinit var button1: Button
    private lateinit var buttonAddHarFile: Button
    private lateinit var buttonApiWebView: Button
    private lateinit var buttonAgentChatV2: Button
    private lateinit var buttonProfileDetail: TextView
    private lateinit var studentName: TextView
    private lateinit var sessionText: TextView
    private lateinit var monthCountText: TextView
    private lateinit var weekCountText: TextView
    private lateinit var monthProgressText: TextView
    private lateinit var calendarTitle: TextView
    private lateinit var calendarMonth: TextView
    private lateinit var todaySummary: TextView
    private lateinit var tagLegendLayout: LinearLayout
    private lateinit var dayCells: List<TextView>
    private val homeMonth: Calendar = Calendar.getInstance().apply { resetTime() }
    private var currentHomeEvents: List<Event> = emptyList()
    private var currentHomeTags: List<Tag> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.menu_buttons, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonLogin = view.findViewById(R.id.button_login)
        buttonViewCalendar2 = view.findViewById(R.id.button_view_calendar2)
        buttonViewCalendar = view.findViewById(R.id.button_view_calendar)
        buttonSession = view.findViewById(R.id.button_session)
        button1 = view.findViewById(R.id.button_1)
        buttonAddHarFile = view.findViewById(R.id.button_add_HAR_file)
        buttonApiWebView = view.findViewById(R.id.button_api_webview)
        buttonAgentChatV2 = view.findViewById(R.id.button_agent_chat_v2)
        buttonProfileDetail = view.findViewById(R.id.button_profile_detail)
        studentName = view.findViewById(R.id.text_home_student_name)
        sessionText = view.findViewById(R.id.text_home_session)
        monthCountText = view.findViewById(R.id.text_home_month_count)
        weekCountText = view.findViewById(R.id.text_home_week_count)
        monthProgressText = view.findViewById(R.id.text_home_month_progress)
        calendarTitle = view.findViewById(R.id.text_home_calendar_title)
        calendarMonth = view.findViewById(R.id.text_home_calendar_month)
        todaySummary = view.findViewById(R.id.text_home_today_summary)
        tagLegendLayout = view.findViewById(R.id.layout_home_tag_legend)
        dayCells = listOf(
            view.findViewById(R.id.home_day_0),
            view.findViewById(R.id.home_day_1),
            view.findViewById(R.id.home_day_2),
            view.findViewById(R.id.home_day_3),
            view.findViewById(R.id.home_day_4),
            view.findViewById(R.id.home_day_5),
            view.findViewById(R.id.home_day_6),
            view.findViewById(R.id.home_day_7),
            view.findViewById(R.id.home_day_8),
            view.findViewById(R.id.home_day_9),
            view.findViewById(R.id.home_day_10),
            view.findViewById(R.id.home_day_11),
            view.findViewById(R.id.home_day_12),
            view.findViewById(R.id.home_day_13),
            view.findViewById(R.id.home_day_14),
            view.findViewById(R.id.home_day_15),
            view.findViewById(R.id.home_day_16),
            view.findViewById(R.id.home_day_17),
            view.findViewById(R.id.home_day_18),
            view.findViewById(R.id.home_day_19),
            view.findViewById(R.id.home_day_20),
            view.findViewById(R.id.home_day_21),
            view.findViewById(R.id.home_day_22),
            view.findViewById(R.id.home_day_23),
            view.findViewById(R.id.home_day_24),
            view.findViewById(R.id.home_day_25),
            view.findViewById(R.id.home_day_26),
            view.findViewById(R.id.home_day_27),
            view.findViewById(R.id.home_day_28),
            view.findViewById(R.id.home_day_29),
            view.findViewById(R.id.home_day_30),
            view.findViewById(R.id.home_day_31),
            view.findViewById(R.id.home_day_32),
            view.findViewById(R.id.home_day_33),
            view.findViewById(R.id.home_day_34),
            view.findViewById(R.id.home_day_35),
            view.findViewById(R.id.home_day_36),
            view.findViewById(R.id.home_day_37),
            view.findViewById(R.id.home_day_38),
            view.findViewById(R.id.home_day_39),
            view.findViewById(R.id.home_day_40),
            view.findViewById(R.id.home_day_41)
        )

        calendarMonth.setOnClickListener {
            showMonthPicker()
        }

        buttonLogin.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_LoginFragment)
        }
        buttonProfileDetail.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_ProfileFragment)
        }
        buttonViewCalendar2.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_CalendarFragment)
        }

        buttonViewCalendar.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_EventsFragment)
        }
        buttonSession.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_SessionFragment)
        }

        button1.setOnClickListener {
            Snackbar.make(view, "Button1", Snackbar.LENGTH_SHORT).show()
        }
        buttonAddHarFile.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_AddHarFileFragment)
        }
        buttonApiWebView.setOnClickListener {
            lifecycleScope.launch {
                val credentials = app.sessionManager.getCredentials()
                if (credentials == null) {
                    Snackbar.make(
                        view,
                        "Login is required before opening WebView",
                        Snackbar.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.action_MenuFragment_to_LoginFragment)
                } else {
                    findNavController().navigate(R.id.action_MenuFragment_to_ApiWebViewFragment)
                }
            }
        }
        buttonAgentChatV2.setOnClickListener {
            lifecycleScope.launch {
                val credentials = app.sessionManager.getCredentials()
                if (credentials == null) {
                    Snackbar.make(
                        view,
                        "Login is required before opening Agent Chat V2",
                        Snackbar.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.action_MenuFragment_to_LoginFragment)
                } else {
                    findNavController().navigate(R.id.action_MenuFragment_to_AgentChatV2Fragment)
                }
            }
        }
        observeHomeData()
    }

    private val app: SmartCalendarApplication
        get() = requireActivity().application as SmartCalendarApplication

    private fun observeHomeData() {
        renderHome(emptyList())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.data.observeSession().collect { session ->
                    renderSession(session)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.data.observeEvents().collect { events ->
                    renderHome(events)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.data.observeTags().collect { tags ->
                    currentHomeTags = tags
                    renderHome(currentHomeEvents)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            app.data.syncNow()
        }
    }

    private fun renderSession(session: SessionInfo?) {
        studentName.text = session?.fullName?.takeIf { it.isNotBlank() }
            ?: session?.username?.takeIf { it.isNotBlank() }
            ?: "Chua dang nhap"
        sessionText.text = if (session == null) {
            "Session: chua dang nhap"
        } else {
            "Session: ${session.username} - het han ${session.expiresAt}"
        }
    }

    private fun renderHome(events: List<Event>) {
        currentHomeEvents = events
        val today = Calendar.getInstance().apply { resetTime() }
        val monthStart = (homeMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            resetTime()
        }
        val gridStart = (monthStart.clone() as Calendar).apply { moveToWeekStart() }
        val monthKey = SimpleDateFormat("MM/yyyy", Locale.US).format(monthStart.time)
        val eventsByDate = events.groupBy { event -> parseBackendTime(event.startTime)?.let(::dateKey) }
        val monthEvents = events.filter { event ->
            parseBackendTime(event.startTime)?.let { sameMonth(it, monthStart) } == true
        }
        val monthEventCount = monthEvents.size
        val monthPassedCount = monthEvents.count { event -> eventHasPassed(event, Calendar.getInstance()) }
        val weekStart = (today.clone() as Calendar).apply { moveToWeekStart() }
        val weekEnd = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
        val weekEventCount = events.count { event ->
            parseBackendTime(event.startTime)?.let { !it.before(weekStart) && it.before(weekEnd) } == true
        }
        val todayEvents = eventsByDate[dateKey(today)].orEmpty()

        calendarTitle.text = "Monthly calendar"
        calendarMonth.text = monthKey
        monthCountText.text = "Lich thang nay: $monthEventCount lich"
        weekCountText.text = "Lich tuan nay: $weekEventCount lich"
        monthProgressText.text = "$monthPassedCount/$monthEventCount"
        todaySummary.text = if (todayEvents.isEmpty()) {
            "Hom nay: chua co lich"
        } else {
            "Hom nay: ${todayEvents.size} lich - ${todayEvents.take(2).joinToString { it.title }}"
        }
        renderTagLegend(monthEvents)

        dayCells.forEachIndexed { index, cell ->
            val day = (gridStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, index) }
            val inMonth = sameMonth(day, monthStart)
            val dayEvents = eventsByDate[dateKey(day)].orEmpty()
                .sortedBy { parseBackendTime(it.startTime)?.timeInMillis ?: Long.MAX_VALUE }
            val eventCount = dayEvents.size
            val isToday = dateKey(day) == dateKey(today)
            val isWeekend = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

            cell.text = buildDayCellText(day.get(Calendar.DAY_OF_MONTH), dayEvents)
            cell.setBackgroundResource(if (isToday) R.drawable.home_day_active_bg else R.drawable.home_day_bg)
            cell.setTextColor(
                when {
                    isToday -> 0xFFFFFFFF.toInt()
                    !inMonth -> 0xFFC4CBD5.toInt()
                    eventCount > 0 -> 0xFF0066A6.toInt()
                    isWeekend -> 0xFFFF6600.toInt()
                    else -> 0xFF333333.toInt()
                }
            )
            cell.textSize = if (eventCount > 0) 11f else 13f
            cell.setOnClickListener {
                showDayEvents(day.clone() as Calendar, dayEvents)
            }
        }
    }

    private fun showMonthPicker() {
        val pickerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), 0)
        }
        val monthPicker = NumberPicker(requireContext()).apply {
            minValue = 1
            maxValue = 12
            value = homeMonth.get(Calendar.MONTH) + 1
            displayedValues = arrayOf(
                "01", "02", "03", "04", "05", "06",
                "07", "08", "09", "10", "11", "12"
            )
        }
        val yearPicker = NumberPicker(requireContext()).apply {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            minValue = currentYear - 5
            maxValue = currentYear + 5
            value = homeMonth.get(Calendar.YEAR)
        }
        pickerLayout.addView(monthPicker)
        pickerLayout.addView(yearPicker)

        AlertDialog.Builder(requireContext())
            .setTitle("Chon thang")
            .setView(pickerLayout)
            .setNegativeButton("Huy", null)
            .setPositiveButton("Chon") { _, _ ->
                homeMonth.set(Calendar.YEAR, yearPicker.value)
                homeMonth.set(Calendar.MONTH, monthPicker.value - 1)
                homeMonth.set(Calendar.DAY_OF_MONTH, 1)
                homeMonth.resetTime()
                renderHome(currentHomeEvents)
            }
            .show()
    }

    private fun showDayEvents(day: Calendar, events: List<Event>) {
        val dialog = AlertDialog.Builder(requireContext()).create()
        val root = layoutInflater.inflate(R.layout.dialog_day_events, null)
        val title = root.findViewById<TextView>(R.id.text_day_events_title)
        val close = root.findViewById<TextView>(R.id.button_day_events_close)
        val emptyText = root.findViewById<TextView>(R.id.text_day_events_empty)
        val list = root.findViewById<LinearLayout>(R.id.layout_day_events_list)

        title.text = "Lich ${weekdayName(day)}, ngay ${SimpleDateFormat("dd/MM/yyyy", Locale.US).format(day.time)}"
        close.setOnClickListener { dialog.dismiss() }

        if (events.isEmpty()) {
            emptyText.visibility = View.VISIBLE
        } else {
            events.forEach { event ->
                list.addView(createEventDetailCard(event, list, dialog))
            }
        }
        dialog.setView(root)
        dialog.show()
    }

    private fun createEventDetailCard(event: Event, parent: ViewGroup, dialog: AlertDialog): View {
        return layoutInflater.inflate(R.layout.item_event_detail, parent, false).apply {
            findViewById<TextView>(R.id.text_event_detail_title).text = event.title
            findViewById<TextView>(R.id.text_event_detail_tag).apply {
                text = "Tag: ${event.tagName.takeIf { it.isNotBlank() && it != "-" } ?: "Su kien"}"
                setOnClickListener {
                    dialog.dismiss()
                    findNavController().navigate(R.id.TagManagerFragment)
                }
            }
            findViewById<TextView>(R.id.text_event_detail_time).text =
                "Thoi gian: ${formatEventTimeRange(event)}"
            findViewById<TextView>(R.id.text_event_detail_detail).apply {
                text = "Chi tiet"
                setOnClickListener {
                    dialog.dismiss()
                    findNavController().navigate(
                        R.id.EventsFragment,
                        Bundle().apply { putString("eventLocalId", event.localId) }
                    )
                }
            }
        }
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

    private fun formatEventTimeRange(event: Event): String {
        val start = parseBackendTime(event.startTime)
        val end = parseBackendTime(event.endTime)
        val formatter = SimpleDateFormat("HH:mm", Locale.US)
        return when {
            start != null && end != null -> "${formatter.format(start.time)} - ${formatter.format(end.time)}"
            start != null -> formatter.format(start.time)
            else -> "-"
        }
    }

    private fun eventHasPassed(event: Event, now: Calendar): Boolean {
        val finishTime = parseBackendTime(event.endTime) ?: parseBackendTime(event.startTime)
        return finishTime?.after(now) == false
    }

    private fun buildDayCellText(dayOfMonth: Int, events: List<Event>): CharSequence {
        if (events.isEmpty()) return dayOfMonth.toString()
        if (events.size >= 4) return "$dayOfMonth\n${events.size} lich"

        val dots = events.joinToString(" ") { "●" }
        val text = "$dayOfMonth\n$dots"
        return SpannableString(text).apply {
            var dotIndex = text.indexOf('●')
            events.forEach { event ->
                if (dotIndex >= 0) {
                    setSpan(
                        ForegroundColorSpan(tagColor(event)),
                        dotIndex,
                        dotIndex + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    dotIndex = text.indexOf('●', dotIndex + 1)
                }
            }
        }
    }

    private fun renderTagLegend(monthEvents: List<Event>) {
        tagLegendLayout.removeAllViews()
        val tags = monthEvents
            .mapNotNull(::tagForEvent)
            .distinctBy { it.localId }
            .take(4)

        if (tags.isEmpty()) {
            addLegendItem("Chua co tag", Color.parseColor("#9CA3AF"))
            return
        }

        tags.forEach { tag ->
            addLegendItem(tag.name, parseTagColor(tag.color, fallbackTagColor(tag.name)))
        }
    }

    private fun addLegendItem(name: String, color: Int) {
        val dot = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(7), dp(7)).apply {
                marginStart = dp(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
        }
        val label = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(6)
            }
            text = name
            setTextColor(Color.parseColor("#6B7280"))
            textSize = 12f
        }
        tagLegendLayout.addView(dot)
        tagLegendLayout.addView(label)
    }

    private fun tagForEvent(event: Event): Tag? {
        return currentHomeTags.firstOrNull { tag ->
            tag.localId == event.tagLocalId ||
                tag.name.equals(event.tagName, ignoreCase = true)
        }
    }

    private fun tagColor(event: Event): Int {
        val tag = tagForEvent(event)
        return parseTagColor(tag?.color, fallbackTagColor(event.tagName))
    }

    private fun parseTagColor(value: String?, fallback: Int): Int {
        return runCatching {
            Color.parseColor(value?.takeIf { it.isNotBlank() } ?: return fallback)
        }.getOrDefault(fallback)
    }

    private fun fallbackTagColor(tagName: String): Int {
        return when (tagName.lowercase(Locale.US)) {
            "hoc", "lich hoc", "study" -> Color.parseColor("#2563EB")
            "thi", "lich thi" -> Color.parseColor("#D97706")
            "su kien", "event" -> Color.parseColor("#059669")
            else -> Color.parseColor("#6B7280")
        }
    }

    private fun weekdayName(day: Calendar): String {
        return when (day.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "thu hai"
            Calendar.TUESDAY -> "thu ba"
            Calendar.WEDNESDAY -> "thu tu"
            Calendar.THURSDAY -> "thu nam"
            Calendar.FRIDAY -> "thu sau"
            Calendar.SATURDAY -> "thu bay"
            else -> "chu nhat"
        }
    }

    private fun sameMonth(first: Calendar, second: Calendar): Boolean =
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.MONTH) == second.get(Calendar.MONTH)

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
}
