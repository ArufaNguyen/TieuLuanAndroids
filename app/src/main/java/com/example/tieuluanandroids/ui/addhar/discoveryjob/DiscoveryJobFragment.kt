package com.example.tieuluanandroids.ui.addhar.discoveryjob

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
import androidx.lifecycle.lifecycleScope
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.AppResult
import com.example.tieuluanandroids.model.DiscoveryJob
import com.example.tieuluanandroids.model.service.SmartCalendarData
import kotlinx.coroutines.launch

class DiscoveryJobFragment : Fragment() {

    private lateinit var buttonRefreshEvents: Button
    private lateinit var textEventState: TextView
    private lateinit var tableEvents: TableLayout
    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.discovery_job_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonRefreshEvents = view.findViewById(R.id.button_refresh_events)
        textEventState = view.findViewById(R.id.text_event_state)
        tableEvents = view.findViewById(R.id.table_events)

        buttonRefreshEvents.setOnClickListener { refresh() }
        renderHeader(showOwner = false)
        refresh()
    }

    private fun refresh() {
        if (isLoading) return

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            when (val result = data.getDiscoveryJobs()) {
                is AppResult.Success -> render(jobs = result.data, message = null)
                is AppResult.Error -> render(jobs = emptyList(), message = result.message)
            }
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        buttonRefreshEvents.isEnabled = !loading
        if (loading) {
            render(jobs = emptyList(), message = null, isLoading = true)
        }
    }

    private fun render(
        jobs: List<DiscoveryJob>,
        message: String?,
        isLoading: Boolean = false
    ) {
        val showOwner = data.isDevMode
        renderHeader(showOwner)
        textEventState.text = when {
            isLoading -> "Dang tai Discovery Jobs..."
            message != null -> message
            jobs.isEmpty() -> "Khong co Discovery Job"
            else -> "Co ${jobs.size} Discovery Job"
        }

        jobs.forEach { job ->
            tableEvents.addView(createJobRow(job, showOwner))
        }
    }

    private fun createJobRow(
        job: DiscoveryJob,
        showOwner: Boolean
    ): TableRow {
        return TableRow(requireContext()).apply {
            addView(cell(job.id))
            addView(cell(job.fileName ?: "-"))
            addView(cell(job.status))
            addView(cell(job.errorMessage ?: "-"))
            addView(cell(job.createdAt))
            addView(cell(job.completedAt ?: "-"))

            if (showOwner) {
                addView(cell(job.userId?.toString() ?: "-"))
            }
        }
    }

    private fun renderHeader(showOwner: Boolean) {
        tableEvents.removeAllViews()
        tableEvents.addView(
            TableRow(requireContext()).apply {
                addView(headerCell("ID"))
                addView(headerCell("File"))
                addView(headerCell("Status"))
                addView(headerCell("Error"))
                addView(headerCell("Created"))
                addView(headerCell("Completed"))

                if (showOwner) {
                    addView(headerCell("Owner ID"))
                }
            }
        )
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

}
