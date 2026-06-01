package com.example.tieuluanandroids

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tieuluanandroids.data.api.EventItem
import com.example.tieuluanandroids.data.api.SmartCalendarApiClient
import com.example.tieuluanandroids.databinding.FragmentFirstBinding
import com.google.android.material.snackbar.Snackbar
import kotlin.concurrent.thread

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonRefreshEvents.setOnClickListener {
            loadEvents()
        }

        renderHeader()
        loadEvents()
    }

    private fun loadEvents() {
        setLoading(true)

        thread {
            val result = SmartCalendarApiClient.getEvents()

            activity?.runOnUiThread {
                if (_binding == null) {
                    return@runOnUiThread
                }

                setLoading(false)

                if (result.success) {
                    renderEvents(result.events)
                } else {
                    renderHeader()
                    binding.textEventState.text = result.message
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renderEvents(events: List<EventItem>) {
        renderHeader()
        binding.textEventState.text = if (events.isEmpty()) {
            getString(R.string.events_empty)
        } else {
            resources.getQuantityString(R.plurals.events_loaded_count, events.size, events.size)
        }

        events.forEach { event ->
            binding.tableEvents.addView(
                TableRow(requireContext()).apply {
                    addView(cell(event.title))
                    addView(cell(formatTimeRange(event)))
                    addView(cell(event.tagName))
                    if (SmartCalendarApiClient.devMode) {
                        addView(cell(event.ownerName))
                    }
                }
            )
        }
    }

    private fun renderHeader() {
        binding.tableEvents.removeAllViews()
        binding.tableEvents.addView(
            TableRow(requireContext()).apply {
                addView(headerCell(getString(R.string.events_column_title)))
                addView(headerCell(getString(R.string.events_column_time)))
                addView(headerCell(getString(R.string.events_column_tag)))
                if (SmartCalendarApiClient.devMode) {
                    addView(headerCell(getString(R.string.events_column_owner)))
                }
            }
        )
    }

    private fun setLoading(isLoading: Boolean) {
        binding.buttonRefreshEvents.isEnabled = !isLoading
        binding.textEventState.text = if (isLoading) {
            getString(R.string.events_loading)
        } else {
            ""
        }
    }

    private fun headerCell(text: String): TextView {
        return cell(text).apply {
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun cell(text: String): TextView {
        val padding = resources.getDimensionPixelSize(R.dimen.event_table_cell_padding)
        return TextView(requireContext()).apply {
            this.text = text
            setPadding(padding, padding, padding, padding)
            minWidth = resources.getDimensionPixelSize(R.dimen.event_table_cell_min_width)
        }
    }

    private fun formatTimeRange(event: EventItem): String {
        return "${event.startTime}\n${event.endTime}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

