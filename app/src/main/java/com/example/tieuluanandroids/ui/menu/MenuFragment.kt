package com.example.tieuluanandroids.ui.menu

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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MenuFragment : Fragment() {

    private lateinit var buttonLogin: Button
    private lateinit var buttonViewCalendar2: Button
    private lateinit var buttonViewCalendar: Button
    private lateinit var buttonSession: Button
    private lateinit var button1: Button
    private lateinit var buttonAddHarFile: Button
    private lateinit var buttonApiWebView: Button
    private lateinit var buttonAgentChatV2: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.menu_buttons, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonLogin = view.findViewById(R.id.button_login)
        buttonViewCalendar2 = view.findViewById(R.id.button_view_calendar2)
        buttonViewCalendar = view.findViewById(R.id.button_view_calendar)
        buttonSession = view.findViewById(R.id.button_session)
        button1 = view.findViewById(R.id.button_1)
        buttonAddHarFile = view.findViewById(R.id.button_add_HAR_file)
        buttonApiWebView = view.findViewById(R.id.button_api_webview)
        buttonAgentChatV2 = view.findViewById(R.id.button_agent_chat_v2)

        buttonLogin.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_LoginFragment)
        }
        buttonViewCalendar2.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_CalendarFragment)
        }

        buttonViewCalendar.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_EventsFragment)
        }
        buttonSession.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_SessionFragment)
        }

        button1.setOnClickListener {
            Snackbar.make(view, "Button1", Snackbar.LENGTH_SHORT).show()
        }
        buttonAddHarFile.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_AddHarFileFragment)
        }
        buttonApiWebView.setOnClickListener {
            lifecycleScope.launch {
                val credentials = app.sessionManager.getCredentials()
                if (credentials == null) {
                    Snackbar.make(
                        view,
                        "Login is required before opening WebView",
                        Snackbar.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.action_MenuFragment_to_LoginFragment)
                } else {
                    findNavController().navigate(R.id.action_MenuFragment_to_ApiWebViewFragment)
                }
            }
        }
        buttonAgentChatV2.setOnClickListener {
            lifecycleScope.launch {
                val credentials = app.sessionManager.getCredentials()
                if (credentials == null) {
                    Snackbar.make(
                        view,
                        "Login is required before opening Agent Chat V2",
                        Snackbar.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.action_MenuFragment_to_LoginFragment)
                } else {
                    findNavController().navigate(R.id.action_MenuFragment_to_AgentChatV2Fragment)
                }
            }
        }
    }

    private val app: SmartCalendarApplication
        get() = requireActivity().application as SmartCalendarApplication
}
