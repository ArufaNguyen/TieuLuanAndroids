package com.example.tieuluanandroids.ui.addhar.discoveryjob

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
import com.example.tieuluanandroids.data.model.DiscoveryJob
import com.example.tieuluanandroids.databinding.DiscoveryJobLayoutBinding
import com.example.tieuluanandroids.ui.ViewModelFactory
import kotlinx.coroutines.launch

class DiscoveryJobFragment : Fragment() {

    private var _binding: DiscoveryJobLayoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiscoveryJobViewModel by viewModels {
        ViewModelFactory {
            DiscoveryJobViewModel(
                (requireActivity().application as SmartCalendarApplication)
                    .repository
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DiscoveryJobLayoutBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonRefreshEvents.setOnClickListener {
            viewModel.refresh()
        }

        renderHeader(showOwner = false)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: DiscoveryJobUiState) {
        binding.buttonRefreshEvents.isEnabled =
            !state.isLoading

        binding.textEventState.text = when {
            state.isLoading ->
                "Đang tải Discovery Jobs..."

            state.message != null ->
                state.message

            state.jobs.isEmpty() ->
                "Không có Discovery Job"

            else ->
                "Có ${state.jobs.size} Discovery Job"
        }

        renderHeader(state.showOwner)

        state.jobs.forEach { job ->
            binding.tableEvents.addView(
                createJobRow(
                    job = job,
                    showOwner = state.showOwner
                )
            )
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
                addView(
                    cell(job.userId?.toString() ?: "-")
                )
            }
        }
    }

    private fun renderHeader(showOwner: Boolean) {
        binding.tableEvents.removeAllViews()

        binding.tableEvents.addView(
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
        val padding = resources.getDimensionPixelSize(
            R.dimen.event_table_cell_padding
        )

        return TextView(requireContext()).apply {
            this.text = text
            setPadding(padding, padding, padding, padding)

            minWidth = resources.getDimensionPixelSize(
                R.dimen.event_table_cell_min_width
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}