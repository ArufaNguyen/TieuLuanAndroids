package com.example.tieuluanandroids.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.databinding.SessionLayoutBinding
import com.example.tieuluanandroids.ui.ViewModelFactory
import kotlinx.coroutines.launch

class SessionFragment : Fragment() {

    private var _binding: SessionLayoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionViewModel by viewModels {
        ViewModelFactory {
            SessionViewModel((requireActivity().application as SmartCalendarApplication).repository)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SessionLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: SessionUiState) {
        val session = state.session
        binding.sessionCheck.text = if (session == null) "not found" else "active"
        binding.sessionCheck2.text = session?.let {
            "${it.username} (userId=${it.userId})\nExpires: ${it.expiresAt}"
        } ?: "not found"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
