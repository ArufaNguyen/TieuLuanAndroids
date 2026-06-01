package com.example.tieuluanandroids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.data.api.SmartCalendarApiClient
import com.example.tieuluanandroids.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import kotlin.concurrent.thread

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

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

            if (username.isBlank() || password.isBlank()) {
                Snackbar.make(binding.root, R.string.login_missing_fields, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)

            thread {
                val result = SmartCalendarApiClient.login(username, password)

                activity?.runOnUiThread {
                    if (_binding == null) {
                        return@runOnUiThread
                    }

                    setLoading(false)

                    if (result.success) {
                        Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment)
                    } else {
                        Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.buttonDevSkip.setOnClickListener {
            setLoading(true)

            thread {
                val result = SmartCalendarApiClient.checkBackendDevMode()

                activity?.runOnUiThread {
                    if (_binding == null) {
                        return@runOnUiThread
                    }

                    setLoading(false)

                    if (result.success) {
                        SmartCalendarApiClient.enableDevMode()
                        findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment)
                    } else {
                        Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setLoading(isLoading: Boolean) {
        binding.buttonLogin.isEnabled = !isLoading
        binding.buttonDevSkip.isEnabled = !isLoading
        binding.editTextUsername.isEnabled = !isLoading
        binding.editTextPassword.isEnabled = !isLoading
        binding.buttonLogin.setText(
            if (isLoading) R.string.login_loading else R.string.login_button
        )
    }
}
