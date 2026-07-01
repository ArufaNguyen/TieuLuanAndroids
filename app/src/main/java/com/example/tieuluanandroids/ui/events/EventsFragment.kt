package com.example.tieuluanandroids.ui.events

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.AppResult
import com.example.tieuluanandroids.model.Event
import com.example.tieuluanandroids.model.service.SmartCalendarData
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class EventsFragment : Fragment() {

    private lateinit var buttonRefreshEvents: Button
    private lateinit var textEventState: TextView
    private lateinit var listEvents: LinearLayout
    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data
    private var isSyncing = false
    private var currentEvents: List<Event> = emptyList()
    private var message: String? = null

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

        buttonRefreshEvents.setOnClickListener { refresh() }
        render(events = currentEvents, isLoading = false, message = null)
        observeEvents()
        refresh()
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                data.observeEvents().collect { events ->
                    currentEvents = events
                    message = null
                    setLoading(false)
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
        listEvents.removeAllViews()
        textEventState.text = when {
            isLoading -> getString(R.string.events_loading)
            message != null -> message
            events.isEmpty() -> getString(R.string.events_empty)
            else -> resources.getQuantityString(
                R.plurals.events_loaded_count,
                events.size,
                events.size
            )
        }

        events.forEach { event ->
            listEvents.addView(eventRow(event, showOwner))
        }
    }

    private fun eventRow(event: Event, showOwner: Boolean): View {
        val padding = dp(12)
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            background = rowBackground()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }

            addView(TextView(context).apply {
                text = event.title.ifBlank { "Untitled event" }
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.rgb(32, 33, 36))
            })
            addView(TextView(context).apply {
                text = formatTimeRange(event)
                textSize = 14f
                setTextColor(Color.rgb(69, 76, 86))
                setPadding(0, dp(6), 0, 0)
            })
            event.description?.takeIf { it.isNotBlank() }?.let { description ->
                addView(TextView(context).apply {
                    text = description
                    textSize = 13f
                    setTextColor(Color.rgb(95, 99, 104))
                    setPadding(0, dp(6), 0, 0)
                })
            }
            addView(TextView(context).apply {
                text = buildMetaText(event, showOwner)
                textSize = 12f
                setTextColor(Color.rgb(95, 99, 104))
                setPadding(0, dp(8), 0, 0)
            })
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
        if (event.syncStatus.name != "SYNCED") parts += event.syncStatus.name
        if (showOwner) parts += event.ownerName.takeIf { it.isNotBlank() } ?: "-"
        return parts.joinToString("  |  ")
    }

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

}
