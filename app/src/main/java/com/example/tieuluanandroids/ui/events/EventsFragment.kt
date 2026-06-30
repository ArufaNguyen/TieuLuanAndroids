package com.example.tieuluanandroids.ui.events

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
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

class EventsFragment : Fragment() {

    private lateinit var buttonRefreshEvents: Button
    private lateinit var textEventState: TextView
    private lateinit var tableEvents: TableLayout
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
        tableEvents = view.findViewById(R.id.table_events)

        buttonRefreshEvents.setOnClickListener { refresh() }
        renderHeader()
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
        renderHeader(showOwner)
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
            tableEvents.addView(
                TableRow(requireContext()).apply {
                    addView(cell(event.title))
                    addView(cell(formatTimeRange(event)))
                    addView(cell(event.tagName))
                    if (showOwner) addView(cell(event.ownerName))
                }
            )
        }
    }

    private fun renderHeader(showOwner: Boolean = false) {
        tableEvents.removeAllViews()
        tableEvents.addView(
            TableRow(requireContext()).apply {
                addView(headerCell(getString(R.string.events_column_title)))
                addView(headerCell(getString(R.string.events_column_time)))
                addView(headerCell(getString(R.string.events_column_tag)))
                if (showOwner) addView(headerCell(getString(R.string.events_column_owner)))
            }
        )
    }

    private fun headerCell(text: String): TextView = cell(text).apply {
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun cell(text: String): TextView {
        val padding = resources.getDimensionPixelSize(R.dimen.event_table_cell_padding)
        return TextView(requireContext()).apply {
            this.text = text
            setPadding(padding, padding, padding, padding)
            minWidth = resources.getDimensionPixelSize(R.dimen.event_table_cell_min_width)
        }
    }

    private fun formatTimeRange(event: Event) = "${event.startTime}\n${event.endTime}"

}
