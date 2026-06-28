package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.AgentAction
import com.example.smartcalendar.agent.dto.AgentPlan
import com.example.smartcalendar.agent.dto.AgentToolCall
import com.example.smartcalendar.agent.dto.AgentToolDescriptor
import com.example.smartcalendar.agent.util.DateParamResolver
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LlmPlanner(
    private val params: DateParamResolver,
    private val json: ObjectMapper,
    private val ninerouter: NinerouterAgentPlannerClient
) {
    private val log = LoggerFactory.getLogger(LlmPlanner::class.java)

    fun plan(
        userMessage: String,
        tools: List<AgentToolDescriptor>,
        currentDate: LocalDate = params.today(),
        timezone: String = "Asia/Ho_Chi_Minh"
    ): AgentPlan {
        val llmPlan = planWithNinerouter(userMessage, tools, currentDate, timezone)
        if (llmPlan != null) return llmPlan

        val forcedTool = tools.singleOrNull()
        val category = forcedTool?.category ?: inferCategory(userMessage)
            ?: return AgentPlan(
                AgentAction.NEED_CLARIFICATION,
                answerHint = "Bạn muốn tôi gọi API nào? Hiện tôi có thể xử lý lịch học, học kỳ, thông báo, môn đã đăng ký hoặc môn cải thiện.",
                reason = "No known category matched."
            )
        val tool = forcedTool ?: selectTool(category, userMessage, tools)
            ?: return AgentPlan(
                AgentAction.NEED_CLARIFICATION,
                answerHint = "Tôi chưa thấy tool phù hợp trong api_knowledge cho yêu cầu này.",
                reason = "No registry tool for $category."
            )
        val resolvedParams = resolveParams(tool, userMessage, currentDate)
        val missing = tool.requiredParams.filterNot { resolvedParams.containsKey(it) }
        if (missing.isNotEmpty()) {
            return AgentPlan(
                AgentAction.NEED_CLARIFICATION,
                answerHint = "Bạn cần cung cấp thêm: ${missing.joinToString(", ")}.",
                reason = "Missing required params: ${missing.joinToString(", ")}."
            )
        }
        val action = when {
            tool.safetyLevel == "READ_ONLY" -> AgentAction.RUN_TOOL
            tool.category == "LOGIN" -> AgentAction.NEED_CONFIRMATION
            tool.safetyLevel == "USER_CONFIRM_REQUIRED" -> AgentAction.NEED_CONFIRMATION
            else -> AgentAction.REFUSED
        }
        return AgentPlan(
            action = action,
            toolCalls = listOf(
                AgentToolCall(
                    toolId = tool.id,
                    toolName = tool.toolName,
                    category = tool.category,
                    params = resolvedParams,
                    headers = defaultHeaders(tool),
                    body = resolveBody(tool, userMessage),
                    confidence = 0.75,
                    reason = "Matched user message to ${tool.category}."
                )
            ),
            reason = "Timezone=$timezone, currentDate=$currentDate"
        )
    }

    private fun planWithNinerouter(
        userMessage: String,
        tools: List<AgentToolDescriptor>,
        currentDate: LocalDate,
        timezone: String
    ): AgentPlan? {
        val response = ninerouter.plan(userMessage, tools, currentDate.toString(), timezone) ?: return null
        val action = runCatching { AgentAction.valueOf(response.action.orEmpty()) }.getOrNull()
            ?: return AgentPlan(
                AgentAction.NEED_CLARIFICATION,
                answerHint = response.answerHint ?: "Model chÆ°a tráº£ vá» Ä‘Æ°á»£c káº¿ hoáº¡ch há»£p lá»‡.",
                reason = response.reason ?: "Invalid planner action from 9router."
            )
        if (action != AgentAction.RUN_TOOL && action != AgentAction.NEED_CONFIRMATION) {
            return AgentPlan(action, answerHint = response.answerHint, reason = response.reason)
        }
        val tool = response.toolId?.let { id -> tools.firstOrNull { it.id == id } }
            ?: response.toolName?.let { name -> tools.firstOrNull { it.toolName.equals(name, ignoreCase = true) } }
            ?: response.category?.let { category ->
                tools.filter { it.category.equals(category, ignoreCase = true) }
                    .sortedWith(
                        compareByDescending<AgentToolDescriptor> { it.readOnly }
                            .thenByDescending { it.id }
                    )
                    .firstOrNull()
            }
        if (tool == null) {
            log.warn("ninerouter_agent_plan_no_matching_tool toolId={} toolName={} category={}", response.toolId, response.toolName, response.category)
            return null
        }
        val params = response.params.filterKeys { it in tool.requiredParams || it in tool.optionalParams }
        val missing = tool.requiredParams.filterNot { params.containsKey(it) }
        if (missing.isNotEmpty()) {
            return AgentPlan(
                AgentAction.NEED_CLARIFICATION,
                answerHint = response.answerHint ?: "Báº¡n cáº§n cung cáº¥p thÃªm: ${missing.joinToString(", ")}.",
                reason = "9router plan missing required params: ${missing.joinToString(", ")}."
            )
        }
        return AgentPlan(
            action = action,
            toolCalls = listOf(
                AgentToolCall(
                    toolId = tool.id,
                    toolName = tool.toolName,
                    category = tool.category,
                    params = params,
                    headers = defaultHeaders(tool) + response.headers,
                    body = response.body?.let { json.valueToTree(it) } ?: resolveBody(tool, userMessage),
                    confidence = 0.9,
                    reason = response.reason ?: "Planned by 9router."
                )
            ),
            answerHint = response.answerHint,
            reason = response.reason ?: "Planned by 9router."
        )
    }

    private fun inferCategory(message: String): String? {
        val text = normalize(message)
        return when {
            listOf("lich", "hoc gi", "tuan nay", "tuan sau").any(text::contains) -> "SCHEDULE"
            listOf("hoc ky", "semester").any(text::contains) -> "SEMESTER"
            listOf("thong bao", "notification").any(text::contains) -> "NOTIFICATION"
            listOf("da dang ky", "dang ky mon nao", "lhp da dang ky").any(text::contains) -> "REGISTERED_COURSES"
            listOf("cai thien", "hoc lai", "retake").any(text::contains) -> "RETAKE_COURSES"
            listOf("login", "dang nhap").any(text::contains) -> "LOGIN"
            else -> null
        }
    }

    private fun selectTool(category: String, message: String, tools: List<AgentToolDescriptor>): AgentToolDescriptor? {
        val candidates = tools.filter { it.category.equals(category, ignoreCase = true) }
        if (candidates.isEmpty()) return null
        val text = normalize(message)
        return candidates.sortedWith(
            compareByDescending<AgentToolDescriptor> { specificityScore(it, text) }
                .thenByDescending { it.readOnly }
                .thenByDescending { it.id }
        ).first()
    }

    private fun specificityScore(tool: AgentToolDescriptor, message: String): Int {
        val url = tool.urlTemplate.lowercase()
        var score = 0
        if ("lichtuan" in url) score += 50
        if ("thang" in url) score += 30
        if ("songayhoc" in url) score -= 30
        if ("category" in url && ("danh muc" in message || "loai" in message)) score += 20
        if ("getnote" in url) score += 15
        if ("notification?" in url && ("trang" in message || "page" in message)) score += 30
        if ("hocky" in url || "hocky" in normalize(tool.toolName)) score += 20
        if ("getlhpdadangky" in url) score += 40
        if ("gethocphancaithien" in url) score += 40
        return score
    }

    private fun resolveParams(tool: AgentToolDescriptor, message: String, currentDate: LocalDate): Map<String, String> {
        val result = linkedMapOf<String, String>()
        tool.requiredParams.forEach { name ->
            val lower = name.lowercase()
            val value = when {
                lower.contains("date") || lower.contains("ngay") -> params.resolveDate(message, currentDate)
                lower == "iddot" || lower.contains("dot") -> params.resolveIdDot(message)
                lower == "categoryid" || lower.contains("category") -> params.resolveCategoryId(message)
                lower == "page" -> params.resolvePage(message) ?: "1"
                lower == "size" -> params.resolveSize(message) ?: "10"
                lower.contains("recaptcha") -> null
                else -> explicitNumberFor(name, message)
            }
            if (value != null) result[name] = value
        }
        return result
    }

    private fun explicitNumberFor(name: String, message: String): String? =
        Regex("""${Regex.escape(name)}\s*[=:]?\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(message)
            ?.groupValues
            ?.get(1)

    private fun defaultHeaders(tool: AgentToolDescriptor): Map<String, String> {
        val required = tool.optionalCredentialHeaders.toSet()
        if (required.isEmpty()) return if (tool.category == "LOGIN") mapOf("Accept-Language" to "vi-VN") else emptyMap()
        val origin = runCatching {
            val uri = java.net.URI(tool.urlTemplate)
            "${uri.scheme}://${uri.host}"
        }.getOrNull().orEmpty()
        return buildMap {
            required.forEach { name ->
                when (name.lowercase()) {
                    "accept" -> put(name, "application/json, text/plain, */*")
                    "accept-language" -> put(name, "vi,en-US;q=0.9,en;q=0.8")
                    "content-type", "content_type" -> put(name, "application/json")
                    "origin" -> if (origin.isNotBlank()) put(name, origin)
                    "referer" -> if (origin.isNotBlank()) put(name, "$origin/")
                    "user-agent" -> put(name, DEFAULT_USER_AGENT)
                    "sec-fetch-dest" -> put(name, "empty")
                    "sec-fetch-mode" -> put(name, "cors")
                    "sec-fetch-site" -> put(name, "same-origin")
                    "sec-ch-ua-mobile" -> put(name, "?0")
                    "sec-ch-ua-platform" -> put(name, "\"Windows\"")
                    "sec-ch-ua" -> put(name, "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
                }
            }
            if (tool.category == "LOGIN" && keys.none { it.equals("Accept-Language", ignoreCase = true) }) {
                put("Accept-Language", "vi-VN")
            }
        }
    }

    private fun resolveBody(tool: AgentToolDescriptor, message: String): com.fasterxml.jackson.databind.JsonNode? {
        if (tool.method !in BODY_METHODS) return null
        val schemaText = tool.bodySchema?.toString().orEmpty()
        if ("username" in schemaText && "password" in schemaText) {
            val username = Regex("""username\s*[:=]\s*([^\s,;]+)""", RegexOption.IGNORE_CASE)
                .find(message)
                ?.groupValues
                ?.get(1)
            val password = Regex("""password\s*[:=]\s*([^\s,;]+)""", RegexOption.IGNORE_CASE)
                .find(message)
                ?.groupValues
                ?.get(1)
            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                return json.valueToTree(mapOf("username" to username, "password" to password))
            }
        }
        return json.nullNode()
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace('đ', 'd')
            .replace('á', 'a').replace('à', 'a').replace('ạ', 'a').replace('ả', 'a').replace('ã', 'a')
            .replace('ắ', 'a').replace('ằ', 'a').replace('ặ', 'a').replace('ẳ', 'a').replace('ẵ', 'a').replace('ă', 'a')
            .replace('ấ', 'a').replace('ầ', 'a').replace('ậ', 'a').replace('ẩ', 'a').replace('ẫ', 'a').replace('â', 'a')
            .replace('é', 'e').replace('è', 'e').replace('ẹ', 'e').replace('ẻ', 'e').replace('ẽ', 'e')
            .replace('ế', 'e').replace('ề', 'e').replace('ệ', 'e').replace('ể', 'e').replace('ễ', 'e').replace('ê', 'e')
            .replace('í', 'i').replace('ì', 'i').replace('ị', 'i').replace('ỉ', 'i').replace('ĩ', 'i')
            .replace('ó', 'o').replace('ò', 'o').replace('ọ', 'o').replace('ỏ', 'o').replace('õ', 'o')
            .replace('ố', 'o').replace('ồ', 'o').replace('ộ', 'o').replace('ổ', 'o').replace('ỗ', 'o').replace('ô', 'o')
            .replace('ớ', 'o').replace('ờ', 'o').replace('ợ', 'o').replace('ở', 'o').replace('ỡ', 'o').replace('ơ', 'o')
            .replace('ú', 'u').replace('ù', 'u').replace('ụ', 'u').replace('ủ', 'u').replace('ũ', 'u')
            .replace('ứ', 'u').replace('ừ', 'u').replace('ự', 'u').replace('ử', 'u').replace('ữ', 'u').replace('ư', 'u')
            .replace('ý', 'y').replace('ỳ', 'y').replace('ỵ', 'y').replace('ỷ', 'y').replace('ỹ', 'y')

    companion object {
        private val BODY_METHODS = setOf("POST", "PUT", "PATCH")
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36"
    }
}
