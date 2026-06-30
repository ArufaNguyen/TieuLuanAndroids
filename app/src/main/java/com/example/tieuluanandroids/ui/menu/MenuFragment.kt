package com.example.tieuluanandroids.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.R
import com.google.android.material.snackbar.Snackbar

class MenuFragment : Fragment() {

    private lateinit var buttonLogin: Button
    private lateinit var buttonViewCalendar2: Button
    private lateinit var buttonViewCalendar: Button
    private lateinit var buttonSession: Button
    private lateinit var button1: Button
    private lateinit var buttonAddHarFile: Button

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
    }
}
