package com.example.smartcalendar.agent.security

import com.example.smartcalendar.agent.dto.AgentAction
import com.example.smartcalendar.agent.dto.AgentPlan
import com.example.smartcalendar.agent.dto.AgentToolDescriptor
import com.example.smartcalendar.agent.dto.PlanValidation
import org.springframework.stereotype.Component

@Component
class SafetyGate {
    fun validate(plan: AgentPlan, tools: List<AgentToolDescriptor>): PlanValidation {
        if (plan.action == AgentAction.NEED_CLARIFICATION) {
            return PlanValidation(false, needsClarification = true, message = plan.answerHint ?: "Bạn cần nói rõ hơn yêu cầu.")
        }
        if (plan.action == AgentAction.REFUSED) {
            return PlanValidation(false, refused = true, message = plan.answerHint ?: "Tôi không thể chạy yêu cầu này.")
        }
        if (plan.toolCalls.size != 1) {
            return PlanValidation(false, needsClarification = true, message = "Hiện tại tôi chỉ xử lý một API mỗi lần. Bạn muốn chạy yêu cầu nào trước?")
        }
        val call = plan.toolCalls.single()
        val tool = tools.firstOrNull { it.id == call.toolId }
            ?: return PlanValidation(false, refused = true, message = "Tool không thuộc registry của user hiện tại.")
        if (!tool.toolName.equals(call.toolName, ignoreCase = true)) {
            return PlanValidation(false, refused = true, message = "Tool planner trả về tên tool không khớp registry.")
        }
        if (call.category != tool.category) {
            return PlanValidation(false, refused = true, message = "Tool planner trả về category không khớp registry.")
        }
        val missing = tool.requiredParams.filterNot(call.params::containsKey)
        if (missing.isNotEmpty()) {
            return PlanValidation(false, needsClarification = true, message = "Bạn cần cung cấp thêm: ${missing.joinToString(", ")}.")
        }
        if (call.params.values.any(::hasUnsafeParamValue)) {
            return PlanValidation(false, refused = true, message = "Tham số có giá trị không an toàn.")
        }
        val blockedHeader = call.headers.keys.firstOrNull(::isBlockedHeader)
        if (blockedHeader != null) {
            return PlanValidation(false, refused = true, message = "Header $blockedHeader không được phép do safety policy.")
        }
        val secretHeader = call.headers.keys.firstOrNull(::isSecretHeader)
        if (secretHeader != null) {
            return PlanValidation(false, refused = true, message = "Planner không được trả về credential header: $secretHeader.")
        }
        if (call.body != null && tool.method !in BODY_METHODS && !call.body.isNull) {
            return PlanValidation(false, refused = true, message = "Body chỉ được dùng cho POST/PUT/PATCH.")
        }
        if (containsCode(plan.reason.orEmpty()) || containsCode(call.reason.orEmpty())) {
            return PlanValidation(false, refused = true, message = "Planner output không được chứa code.")
        }
        return when {
            tool.safetyLevel == "READ_ONLY" -> PlanValidation(true)
            tool.category == "LOGIN" -> PlanValidation(false, needsConfirmation = true, message = "Đây là thao tác login portal. Bạn xác nhận muốn chạy API login không?")
            tool.safetyLevel == "USER_CONFIRM_REQUIRED" -> PlanValidation(false, needsConfirmation = true, message = "Đây là thao tác có thể thay đổi dữ liệu. Bạn cần xác nhận trước khi chạy.")
            else -> PlanValidation(false, refused = true, message = "Tool này bị chặn bởi safety policy.")
        }
    }

    private fun hasUnsafeParamValue(value: String): Boolean {
        val lowered = value.lowercase()
        return lowered.contains("://") || lowered.contains("\r") || lowered.contains("\n")
    }

    private fun isBlockedHeader(name: String): Boolean =
        name.lowercase() in BLOCKED_HEADERS

    private fun isSecretHeader(name: String): Boolean {
        val lower = name.lowercase()
        return lower == "authorization" ||
            lower == "cookie" ||
            lower == "set-cookie" ||
            lower.contains("csrf") ||
            lower.contains("xsrf") ||
            lower.contains("password") ||
            lower.contains("token")
    }

    private fun containsCode(value: String): Boolean {
        val lower = value.lowercase()
        return "```" in value ||
            "curl " in lower ||
            "javascript" in lower ||
            "python" in lower ||
            "kotlin" in lower ||
            "fetch(" in lower
    }

    companion object {
        private val BODY_METHODS = setOf("POST", "PUT", "PATCH")
        private val BLOCKED_HEADERS = setOf("host", "content-length", "connection", "transfer-encoding")
    }
}

