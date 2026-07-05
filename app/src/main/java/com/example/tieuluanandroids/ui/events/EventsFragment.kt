package com.example.tieuluanandroids.ui.events

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.AppResult
import com.example.tieuluanandroids.model.Event
import com.example.tieuluanandroids.model.Tag
import com.example.tieuluanandroids.model.UpdateEventInput
import com.example.tieuluanandroids.model.service.SmartCalendarData
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class EventsFragment : Fragment() {

    private lateinit var buttonRefreshEvents: Button
    private lateinit var textEventState: TextView
    private lateinit var listEvents: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var emptyText: TextView
    private lateinit var closeButton: Button
    private lateinit var editorTitleText: TextView
    private lateinit var titleInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var tagSpinner: Spinner
    private lateinit var startInput: EditText
    private lateinit var endInput: EditText
    private lateinit var saveEditButton: Button
    private lateinit var clearEditButton: Button
    private lateinit var deleteEditButton: Button
    private lateinit var tagAdapter: ArrayAdapter<String>
    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data
    private var isSyncing = false
    private var currentEvents: List<Event> = emptyList()
    private var currentTags: List<Tag> = emptyList()
    private var message: String? = null
    private var searchQuery: String = ""
    private var pendingEventLocalId: String? = null
    private var editingEvent: Event? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_events, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonRefreshEvents = view.findViewById(R.id.button_refresh_events)
        textEventState = view.findViewById(R.id.text_event_state)
        listEvents = view.findViewById(R.id.list_events)
        searchInput = view.findViewById(R.id.edt_search_events)
        emptyText = view.findViewById(R.id.text_event_empty)
        closeButton = view.findViewById(R.id.btn_close_events)
        editorTitleText = view.findViewById(R.id.text_event_editor_title)
        titleInput = view.findViewById(R.id.edt_event_title)
        descriptionInput = view.findViewById(R.id.edt_event_description)
        tagSpinner = view.findViewById(R.id.spinner_event_tag)
        startInput = view.findViewById(R.id.edt_event_start_time)
        endInput = view.findViewById(R.id.edt_event_end_time)
        saveEditButton = view.findViewById(R.id.btn_save_event_edit)
        clearEditButton = view.findViewById(R.id.btn_clear_event_edit)
        deleteEditButton = view.findViewById(R.id.btn_delete_event_edit)

        tagAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf<String>())
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tagSpinner.adapter = tagAdapter

        buttonRefreshEvents.setOnClickListener { refresh() }
        closeButton.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        saveEditButton.setOnClickListener { saveEditedEvent() }
        clearEditButton.setOnClickListener { clearEditor() }
        deleteEditButton.setOnClickListener {
            editingEvent?.let(::confirmDeleteEvent)
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                render(events = currentEvents, isLoading = isSyncing, message = message)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        pendingEventLocalId = arguments?.getString(ARG_EVENT_LOCAL_ID)
        arguments?.remove(ARG_EVENT_LOCAL_ID)
        clearEditor()
        render(events = currentEvents, isLoading = false, message = null)
        observeEvents()
        observeTags()
        refresh()
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                data.observeEvents().collect { events ->
                    currentEvents = events
                    message = null
                    setLoading(false)
                    openPendingEventIfReady()
                }
            }
        }
    }

    private fun observeTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                data.observeTags().collect { tags ->
                    currentTags = tags
                    updateTagAdapter(editingEvent)
                    render(events = currentEvents, isLoading = isSyncing, message = message)
                    openPendingEventIfReady()
                }
            }
        }
    }

    private fun refresh() {
        if (isSyncing) return

        viewLifecycleOwner.lifecycleScope.launch {
            isSyncing = true
            setLoading(true)
            when (val result = data.syncNow()) {
                is AppResult.Success -> setLoading(false)
                is AppResult.Error -> {
                    message = result.message
                    setLoading(false)
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_LONG).show()
                }
            }
            isSyncing = false
        }
    }

    private fun setLoading(loading: Boolean) {
        buttonRefreshEvents.isEnabled = !loading
        render(events = currentEvents, isLoading = loading, message = message)
    }

    private fun render(events: List<Event>, isLoading: Boolean, message: String?) {
        val showOwner = data.isDevMode
        val filtered = events.filter { event -> event.matches(searchQuery) }
        listEvents.removeAllViews()
        textEventState.text = when {
            isLoading -> getString(R.string.events_loading)
            message != null -> message
            events.isEmpty() -> getString(R.string.events_empty)
            filtered.isEmpty() -> "Khong tim thay su kien phu hop."
            else -> resources.getQuantityString(
                R.plurals.events_loaded_count,
                filtered.size,
                filtered.size
            )
        }
        emptyText.isVisible = !isLoading && filtered.isEmpty()
        emptyText.text = if (events.isEmpty()) {
            getString(R.string.events_empty)
        } else {
            "Khong tim thay su kien phu hop."
        }

        filtered.forEach { event ->
            listEvents.addView(eventRow(event, showOwner))
        }
    }

    private fun eventRow(event: Event, showOwner: Boolean): View {
        val padding = dp(12)
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(76)
            setPadding(padding, padding, padding, padding)
            background = rowBackground()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }

            addView(View(context).apply {
                setBackgroundColor(tagColor(event.tagName))
            }, LinearLayout.LayoutParams(dp(12), dp(52)))

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
                addView(TextView(context).apply {
                    text = event.title.ifBlank { "Untitled event" }
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.rgb(17, 24, 39))
                })
                addView(TextView(context).apply {
                    text = formatTimeRange(event)
                    textSize = 13f
                    setTextColor(Color.rgb(75, 85, 99))
                    setPadding(0, dp(2), 0, 0)
                })
                addView(TextView(context).apply {
                    text = buildMetaText(event, showOwner)
                    textSize = 12f
                    setTextColor(Color.rgb(107, 114, 128))
                    setPadding(0, dp(2), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            addView(Button(context).apply {
                text = "Sua"
                textSize = 12f
                isAllCaps = false
                setOnClickListener { editEvent(event) }
            }, LinearLayout.LayoutParams(dp(64), dp(40)))

            addView(Button(context).apply {
                text = "Xoa"
                textSize = 12f
                isAllCaps = false
                setOnClickListener { confirmDeleteEvent(event) }
            }, LinearLayout.LayoutParams(dp(64), dp(40)).apply { leftMargin = dp(6) })
        }
    }

    private fun rowBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(8).toFloat()
            setStroke(dp(1), Color.rgb(224, 229, 235))
        }
    }

    private fun buildMetaText(event: Event, showOwner: Boolean): String {
        val parts = mutableListOf<String>()
        parts += event.tagName.takeIf { it.isNotBlank() } ?: "-"
        if (!event.isSynced) parts += event.syncStatus.name
        if (showOwner) parts += event.ownerName.takeIf { it.isNotBlank() } ?: "-"
        return parts.joinToString("  |  ")
    }

    private fun Event.matches(query: String): Boolean {
        val normalized = query.trim()
        if (normalized.isBlank()) return true
        return title.contains(normalized, ignoreCase = true) ||
            tagName.contains(normalized, ignoreCase = true) ||
            description.orEmpty().contains(normalized, ignoreCase = true) ||
            ownerName.contains(normalized, ignoreCase = true)
    }

    private fun openPendingEventIfReady() {
        val localId = pendingEventLocalId ?: return
        val event = currentEvents.firstOrNull { it.localId == localId } ?: return
        pendingEventLocalId = null
        editEvent(event)
    }

    private fun editEvent(event: Event) {
        editingEvent = event
        editorTitleText.text = "Sua su kien"
        titleInput.setText(event.title)
        descriptionInput.setText(event.description.orEmpty())
        startInput.setText(event.startTime)
        endInput.setText(event.endTime)
        deleteEditButton.isVisible = true
        updateTagAdapter(event)
    }

    private fun saveEditedEvent() {
        val event = editingEvent
        if (event == null) {
            Toast.makeText(requireContext(), "Hay chon mot su kien de sua", Toast.LENGTH_SHORT).show()
            return
        }
        val title = titleInput.text.toString().trim()
        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Ten su kien dang trong", Toast.LENGTH_SHORT).show()
            return
        }
        updateEvent(
            UpdateEventInput(
                localId = event.localId,
                title = title,
                description = descriptionInput.text.toString().trim().ifBlank { null },
                startTime = startInput.text.toString().trim(),
                endTime = endInput.text.toString().trim(),
                tagLocalId = selectedTagLocalId()
            )
        )
    }

    private fun updateEvent(input: UpdateEventInput) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = data.updateEvent(input)) {
                is AppResult.Success -> {
                    Toast.makeText(requireContext(), "Da cap nhat su kien", Toast.LENGTH_SHORT).show()
                    clearEditor()
                }
                is AppResult.Error -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmDeleteEvent(event: Event) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xoa su kien")
            .setMessage("Ban co chac muon xoa \"${event.title}\"?")
            .setNegativeButton("Huy", null)
            .setPositiveButton("Xoa") { _, _ -> deleteEvent(event) }
            .show()
    }

    private fun deleteEvent(event: Event) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = data.deleteEvent(event.localId)) {
                is AppResult.Success -> {
                    Toast.makeText(requireContext(), "Da xoa su kien", Toast.LENGTH_SHORT).show()
                    if (editingEvent?.localId == event.localId) clearEditor()
                }
                is AppResult.Error -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearEditor() {
        editingEvent = null
        editorTitleText.text = "Sua su kien"
        titleInput.text?.clear()
        descriptionInput.text?.clear()
        startInput.text?.clear()
        endInput.text?.clear()
        deleteEditButton.isVisible = false
        updateTagAdapter(null)
    }

    private fun updateTagAdapter(selectedEvent: Event?) {
        val names = tagOptions().map { it.name }
        tagAdapter.clear()
        tagAdapter.addAll(names)
        tagAdapter.notifyDataSetChanged()
        val selectedIndex = selectedEvent?.let { event ->
            tagOptions().indexOfFirst { tag ->
                tag.localId == event.tagLocalId || tag.name.equals(event.tagName, ignoreCase = true)
            }
        } ?: -1
        if (selectedIndex >= 0) tagSpinner.setSelection(selectedIndex)
    }

    private fun tagOptions(): List<Tag> {
        return currentTags.ifEmpty {
            listOf(Tag("", null, "Khong co tag", null, null, editingEvent?.syncStatus ?: currentEvents.firstOrNull()?.syncStatus ?: com.example.tieuluanandroids.model.SyncStatus.SYNCED, 0L, null))
        }
    }

    private fun selectedTagLocalId(): String? =
        tagOptions().getOrNull(tagSpinner.selectedItemPosition)?.localId?.takeIf { it.isNotBlank() }

    private fun formatTimeRange(event: Event): String {
        val start = parseBackendTime(event.startTime)
        val end = parseBackendTime(event.endTime)
        if (start != null && end != null) {
            val dateText = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(start)
            val startText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(start)
            val endText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(end)
            return "$dateText | $startText-$endText"
        }
        return "${event.startTime}\n${event.endTime}"
    }

    private fun parseBackendTime(value: String): java.util.Date? {
        val normalized = value.trim().removeSuffix("Z")
        val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm:ss")
        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching { SimpleDateFormat(pattern, Locale.US).parse(normalized) }.getOrNull()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun tagColor(tagName: String): Int {
        return when (tagName.lowercase(Locale.US)) {
            "hoc", "lich hoc" -> Color.parseColor("#2563EB")
            "thi", "lich thi" -> Color.parseColor("#D97706")
            "su kien" -> Color.parseColor("#059669")
            else -> Color.parseColor("#6B7280")
        }
    }

    companion object {
        private const val ARG_EVENT_LOCAL_ID = "eventLocalId"
    }

}
