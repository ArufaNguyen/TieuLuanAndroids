package com.example.tieuluanandroids.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.model.SessionInfo
import com.example.tieuluanandroids.model.service.SmartCalendarData
import kotlinx.coroutines.launch

class SessionFragment : Fragment() {

    private lateinit var sessionCheck: TextView
    private lateinit var sessionCheck2: TextView
    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.session_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionCheck = view.findViewById(R.id.sessionCheck)
        sessionCheck2 = view.findViewById(R.id.sessionCheck2)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                data.observeSession().collect(::render)
            }
        }
    }

    private fun render(session: SessionInfo?) {
        sessionCheck.text = if (session == null) "not found" else "active"
        sessionCheck2.text = session?.let {
            "${it.username} (userId=${it.userId})\nExpires: ${it.expiresAt}"
        } ?: "not found"
    }
}
