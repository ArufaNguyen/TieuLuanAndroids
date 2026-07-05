package com.example.tieuluanandroids.ui.agent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.service.AgentChatResponse
import com.example.tieuluanandroids.model.service.AgentToolCall
import com.example.tieuluanandroids.model.service.SmartCalendarData
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AgentChatV2Fragment : Fragment() {

    private lateinit var editMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var buttonConfirm: Button
    private lateinit var textState: TextView
    private lateinit var textAnswer: TextView
    private lateinit var textToolCalls: TextView

    private var pendingMessage: String? = null
    private var isSending = false

    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data

    private val app: SmartCalendarApplication
        get() = requireActivity().application as SmartCalendarApplication

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_agent_chat_v2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editMessage = view.findViewById(R.id.edit_agent_message)
        buttonSend = view.findViewById(R.id.button_send_agent_chat)
        buttonConfirm = view.findViewById(R.id.button_confirm_agent_chat)
        textState = view.findViewById(R.id.text_agent_state)
        textAnswer = view.findViewById(R.id.text_agent_answer)
        textToolCalls = view.findViewById(R.id.text_agent_tool_calls)

        buttonSend.setOnClickListener { send(confirmed = false) }
        buttonConfirm.setOnClickListener { send(confirmed = true) }

        viewLifecycleOwner.lifecycleScope.launch {
            if (app.sessionManager.getCredentials() == null) {
                Snackbar.make(
                    requireView(),
                    "Login is required before opening Agent Chat V2",
                    Snackbar.LENGTH_LONG
                ).show()
                findNavController().navigate(R.id.LoginFragment)
            }
        }
    }

    private fun send(confirmed: Boolean) {
        if (isSending) return

        val message = if (confirmed) {
            pendingMessage ?: editMessage.text.toString().trim()
        } else {
            editMessage.text.toString().trim()
        }

        if (message.isBlank()) {
            Snackbar.make(requireView(), "Enter a message first", Snackbar.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            setSending(true)
            textState.text = if (confirmed) "Confirming run..." else "Sending..."
            val result = data.agentChatV2(message, confirmed)
            setSending(false)

            if (!result.success) {
                textState.text = result.message
                Snackbar.make(requireView(), result.message, Snackbar.LENGTH_LONG).show()
                return@launch
            }

            val response = result.response
            if (response == null) {
                textState.text = "Empty agent response"
                return@launch
            }
            renderResponse(message, response)
        }
    }

    private fun renderResponse(message: String, response: AgentChatResponse) {
        textAnswer.text = response.answer.ifBlank { "(No answer)" }
        textToolCalls.text = renderToolCalls(response.toolCalls)

        pendingMessage = if (response.needsConfirmation) message else null
        buttonConfirm.visibility = if (response.needsConfirmation) View.VISIBLE else View.GONE

        textState.text = when {
            response.needsConfirmation -> "Confirmation required"
            response.missing.isNotEmpty() -> "Missing: ${response.missing.joinToString()}"
            response.needsClarification -> clarificationState(response.answer)
            else -> "Done"
        }
    }

    private fun clarificationState(answer: String): String {
        return if (answer.isBlank()) {
            "Clarification required"
        } else {
            "Clarification required: $answer"
        }
    }

    private fun renderToolCalls(toolCalls: List<AgentToolCall>): String {
        if (toolCalls.isEmpty()) return "No tool calls."
        return toolCalls.joinToString(separator = "\n\n") { call ->
            buildString {
                append("#")
                append(call.toolId ?: "?")
                append(" ")
                append(call.toolName.ifBlank { "(unknown tool)" })
                if (call.category.isNotBlank()) {
                    append(" [")
                    append(call.category)
                    append("]")
                }
                call.upstreamStatus?.let {
                    append("\nStatus: ")
                    append(it)
                }
                if (call.params.isNotEmpty()) {
                    append("\nParams:")
                    call.params.forEach { (key, value) ->
                        append("\n- ")
                        append(key)
                        append(": ")
                        append(value)
                    }
                }
            }
        }
    }

    private fun setSending(sending: Boolean) {
        isSending = sending
        buttonSend.isEnabled = !sending
        buttonConfirm.isEnabled = !sending
        editMessage.isEnabled = !sending
    }
}
