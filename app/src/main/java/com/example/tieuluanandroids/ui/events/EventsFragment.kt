package com.example.tieuluanandroids.ui.events

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.data.model.Event
import com.example.tieuluanandroids.databinding.FragmentEventsBinding
import com.example.tieuluanandroids.ui.ViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EventsViewModel by viewModels {
        ViewModelFactory {
            EventsViewModel((requireActivity().application as SmartCalendarApplication).repository)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonRefreshEvents.setOnClickListener { viewModel.refresh() }
        renderHeader()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.errors.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun render(state: EventsUiState) {
        binding.buttonRefreshEvents.isEnabled = !state.isLoading
        renderHeader(state.showOwner)
        binding.textEventState.text = when {
            state.isLoading -> getString(R.string.events_loading)
            state.message != null -> state.message
            state.events.isEmpty() -> getString(R.string.events_empty)
            else -> resources.getQuantityString(
                R.plurals.events_loaded_count,
                state.events.size,
                state.events.size
            )
        }

        state.events.forEach { event ->
            binding.tableEvents.addView(
                TableRow(requireContext()).apply {
                    addView(cell(event.title))
                    addView(cell(formatTimeRange(event)))
                    addView(cell(event.tagName))
                    if (state.showOwner) addView(cell(event.ownerName))
                }
            )
        }
    }

    private fun renderHeader(showOwner: Boolean = false) {
        binding.tableEvents.removeAllViews()
        binding.tableEvents.addView(
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
