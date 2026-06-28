package com.example.tieuluanandroids.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.databinding.FragmentLoginBinding
import com.example.tieuluanandroids.ui.ViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels {
        ViewModelFactory {
            LoginViewModel((requireActivity().application as SmartCalendarApplication).repository)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonLogin.setOnClickListener {
            val username = binding.editTextUsername.text?.toString()?.trim().orEmpty()
            val password = binding.editTextPassword.text?.toString()?.trim().orEmpty()
            viewModel.login(username, password)
        }
        binding.buttonDevSkip.setOnClickListener {
            viewModel.continueInDevMode()
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: LoginUiState) {
        binding.buttonLogin.isEnabled = !state.isLoading
        binding.buttonDevSkip.isEnabled = !state.isLoading
        binding.editTextUsername.isEnabled = !state.isLoading
        binding.editTextPassword.isEnabled = !state.isLoading
        binding.buttonLogin.setText(
            if (state.isLoading) R.string.login_loading else R.string.login_button
        )
    }

    private fun handleEvent(event: LoginEvent) {
        when (event) {
            LoginEvent.MissingFields -> Snackbar.make(
                binding.root,
                R.string.login_missing_fields,
                Snackbar.LENGTH_SHORT
            ).show()
            is LoginEvent.ShowMessage -> Snackbar.make(
                binding.root,
                event.message,
                Snackbar.LENGTH_LONG
            ).show()
            LoginEvent.NavigateToEvents -> findNavController().navigate(
                R.id.action_LoginFragment_to_EventsFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
