package com.example.tieuluanandroids.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.SessionInfo
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var statusText: TextView
    private lateinit var fullNameText: TextView
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var userIdText: TextView
    private lateinit var expiresText: TextView
    private lateinit var loginButton: Button

    private val app: SmartCalendarApplication
        get() = requireActivity().application as SmartCalendarApplication

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusText = view.findViewById(R.id.text_profile_status)
        fullNameText = view.findViewById(R.id.text_profile_full_name)
        usernameText = view.findViewById(R.id.text_profile_username)
        emailText = view.findViewById(R.id.text_profile_email)
        userIdText = view.findViewById(R.id.text_profile_user_id)
        expiresText = view.findViewById(R.id.text_profile_expires)
        loginButton = view.findViewById(R.id.button_profile_login)

        loginButton.setOnClickListener {
            findNavController().navigate(R.id.LoginFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.data.observeSession().collect(::render)
            }
        }
    }

    private fun render(session: SessionInfo?) {
        if (session == null) {
            statusText.text = "Chua dang nhap"
            fullNameText.text = "-"
            usernameText.text = "-"
            emailText.text = "-"
            userIdText.text = "-"
            expiresText.text = "-"
            loginButton.visibility = View.VISIBLE
            return
        }

        statusText.text = "Dang hoat dong"
        fullNameText.text = session.fullName?.takeIf { it.isNotBlank() } ?: "-"
        usernameText.text = session.username.ifBlank { session.loginName ?: "-" }
        emailText.text = session.email.ifBlank { "-" }
        userIdText.text = "userId=${session.userId}, accountId=${session.accountId}"
        expiresText.text = session.expiresAt
        loginButton.visibility = View.GONE
    }
}
