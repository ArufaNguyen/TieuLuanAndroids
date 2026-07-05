package com.example.tieuluanandroids.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.service.SmartCalendarData
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var buttonLogin: Button
    private lateinit var buttonDevSkip: Button
    private lateinit var editTextUsername: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonLogin = view.findViewById(R.id.button_login)
        buttonDevSkip = view.findViewById(R.id.button_dev_skip)
        editTextUsername = view.findViewById(R.id.edit_text_username)
        editTextPassword = view.findViewById(R.id.edit_text_password)

        buttonLogin.setOnClickListener {
            val username = editTextUsername.text?.toString()?.trim().orEmpty()
            val password = editTextPassword.text?.toString()?.trim().orEmpty()
            login(username, password)
        }
        buttonDevSkip.setOnClickListener {
            val username = editTextUsername.text?.toString()?.trim().orEmpty()
            val password = editTextPassword.text?.toString()?.trim().orEmpty()
            register(username, password)
        }
    }

    private fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            showMessage(getString(R.string.login_missing_fields), Snackbar.LENGTH_SHORT)
            return
        }
        if (isLoading) return

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val result = data.login(username, password)
            setLoading(false)
            showMessage(result.message, Snackbar.LENGTH_LONG)
            if (result.success) {
                findNavController().navigate(R.id.action_LoginFragment_to_EventsFragment)
            }
        }
    }

    private fun register(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            showMessage(getString(R.string.login_missing_fields), Snackbar.LENGTH_SHORT)
            return
        }
        if (isLoading) return

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val result = data.register(username, password)
            setLoading(false)
            showMessage(result.message, Snackbar.LENGTH_LONG)
            if (result.success) {
                findNavController().navigate(R.id.action_LoginFragment_to_EventsFragment)
            }
        }
    }

    private fun continueInDevMode() {
        if (isLoading) return

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val result = data.checkBackendDevMode()
            if (result.success) {
                data.enableDevMode()
            }
            setLoading(false)
            if (result.success) {
                findNavController().navigate(R.id.action_LoginFragment_to_EventsFragment)
            } else {
                showMessage(result.message, Snackbar.LENGTH_LONG)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        buttonLogin.isEnabled = !loading
        buttonDevSkip.isEnabled = !loading
        editTextUsername.isEnabled = !loading
        editTextPassword.isEnabled = !loading
        buttonLogin.setText(
            if (loading) R.string.login_loading else R.string.login_button
        )
    }

    private fun showMessage(message: String, duration: Int) {
        Snackbar.make(requireView(), message, duration).show()
    }
}
