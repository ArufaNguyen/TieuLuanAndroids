package com.example.tieuluanandroids.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var oldPasswordEdit: EditText
    private lateinit var newPasswordEdit: EditText
    private lateinit var confirmPasswordEdit: EditText
    private lateinit var changePasswordButton: Button

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
        oldPasswordEdit = view.findViewById(R.id.edit_profile_old_password)
        newPasswordEdit = view.findViewById(R.id.edit_profile_new_password)
        confirmPasswordEdit = view.findViewById(R.id.edit_profile_confirm_password)
        changePasswordButton = view.findViewById(R.id.button_profile_change_password)

        loginButton.setOnClickListener {
            findNavController().navigate(R.id.LoginFragment)
        }
        changePasswordButton.setOnClickListener {
            changePassword()
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
            setPasswordFormEnabled(false)
            clearPasswordInputs()
            return
        }

        statusText.text = "Dang hoat dong"
        fullNameText.text = session.fullName?.takeIf { it.isNotBlank() } ?: "-"
        usernameText.text = session.username.ifBlank { session.loginName ?: "-" }
        emailText.text = session.email.ifBlank { "-" }
        userIdText.text = "userId=${session.userId}, accountId=${session.accountId}"
        expiresText.text = session.expiresAt
        loginButton.visibility = View.GONE
        setPasswordFormEnabled(true)
    }

    private fun changePassword() {
        val oldPassword = oldPasswordEdit.text?.toString().orEmpty()
        val newPassword = newPasswordEdit.text?.toString().orEmpty()
        val confirmPassword = confirmPasswordEdit.text?.toString().orEmpty()

        when {
            oldPassword.isBlank() -> {
                oldPasswordEdit.error = "Nhap mat khau cu"
                oldPasswordEdit.requestFocus()
                return
            }
            newPassword.isBlank() -> {
                newPasswordEdit.error = "Nhap mat khau moi"
                newPasswordEdit.requestFocus()
                return
            }
            confirmPassword != newPassword -> {
                confirmPasswordEdit.error = "Mat khau moi khong khop"
                confirmPasswordEdit.requestFocus()
                return
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            setPasswordFormEnabled(false)
            val result = app.data.changePassword(oldPassword, newPassword)
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            if (result.success) {
                clearPasswordInputs()
            }
            setPasswordFormEnabled(true)
        }
    }

    private fun setPasswordFormEnabled(enabled: Boolean) {
        oldPasswordEdit.isEnabled = enabled
        newPasswordEdit.isEnabled = enabled
        confirmPasswordEdit.isEnabled = enabled
        changePasswordButton.isEnabled = enabled
    }

    private fun clearPasswordInputs() {
        oldPasswordEdit.text?.clear()
        newPasswordEdit.text?.clear()
        confirmPasswordEdit.text?.clear()
    }
}
