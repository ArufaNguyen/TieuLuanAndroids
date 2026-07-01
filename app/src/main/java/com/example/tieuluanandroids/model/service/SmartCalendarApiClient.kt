package com.example.tieuluanandroids.model.service

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.local.*
import com.example.tieuluanandroids.model.sync.*

import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

object SmartCalendarApiClient {

    private const val API_URL_SOURCE =
        "https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(cookieManager))
        .connectTimeout(0, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    var devMode: Boolean = false
        private set

    fun login(username: String, password: String): LoginApiResult = try {
        val payload = JSONObject()
            .put("loginName", username)
            .put("password", password)
            .toString()
        val request = Request.Builder()
            .url("${fetchBaseUrl()}/api/v1/auth/login")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return LoginApiResult(false, "HTTP ${response.code}: $responseBody")
            }
            val json = JSONObject(responseBody)
            val message = json.optString("message", "unknown response")
            if (json.optInt("code", response.code) != 200) {
                return LoginApiResult(false, message)
            }
            val body = json.optJSONObject("body")
                ?: return LoginApiResult(false, "Login response has no body")
            val token = body.optString("sessionToken")
            if (token.isBlank()) return LoginApiResult(false, "Login response has no session token")

            devMode = false
            LoginApiResult(
                success = true,
                message = "Login success",
                token = token,
                session = SessionInfo(
                    accountId = body.optInt("accountId"),
                    userId = body.optInt("userId"),
                    username = body.optString("username"),
                    loginName = body.nullableString("loginName"),
                    email = body.optString("email"),
                    fullName = body.nullableString("fullName"),
                    expiresAt = body.optString("expiresAt")
                )
            )
        }
    } catch (error: Exception) {
        LoginApiResult(false, error.message ?: "Network error")
    }

    fun enableDevMode() {
        devMode = true
    }

    fun checkBackendDevMode(token: String): ApiResult {
        return try {
            val baseUrl = fetchBaseUrl()
            val request = authenticatedRequest("$baseUrl/api/v1/auth/me", token)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return ApiResult(false, "HTTP ${response.code}: $responseBody")
                }

                val json = JSONObject(responseBody)
                if (json.optInt("code", response.code) == 200) {
                    ApiResult(true, "Session is valid")
                } else {
                    ApiResult(false, json.optString("message", "Invalid session"))
                }
            }
        } catch (error: Exception) {
            ApiResult(false, error.message ?: "Network error")
        }
    }

    fun getRemoteEvents(token: String): RemoteListResult<RemoteEvent> {
        return try {
            val baseUrl = fetchBaseUrl()
            val request = authenticatedRequest("$baseUrl/api/v1/events", token)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return RemoteListResult(false, "HTTP ${response.code}: $responseBody", emptyList())
                }

                val json = JSONObject(responseBody)
                val code = json.optInt("code", response.code)
                val message = json.optString("message", "unknown response")
                if (code != 200) {
                    return RemoteListResult(false, message, emptyList())
                }

                RemoteListResult(true, message, RemoteJsonParser.parseEvents(json.optJSONArray("body")))
            }
        } catch (error: Exception) {
            RemoteListResult(false, error.message ?: "Network error", emptyList())
        }
    }

    fun getRemoteTags(token: String): RemoteListResult<RemoteTag> {
        return try {
            val request = authenticatedRequest("${fetchBaseUrl()}/api/v1/tags", token)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return RemoteListResult(false, "HTTP ${response.code}: $responseBody", emptyList())
                }
                val json = JSONObject(responseBody)
                val code = json.optInt("code", response.code)
                val message = json.optString("message", "unknown response")
                if (code !in 200..299) return RemoteListResult(false, message, emptyList())
                RemoteListResult(true, message, RemoteJsonParser.parseTags(json.optJSONArray("body")))
            }
        } catch (error: Exception) {
            RemoteListResult(false, error.message ?: "Network error", emptyList())
        }
    }

    fun createRemoteEvent(token: String, payload: JSONObject): RemoteWriteResult =
        writeJson("/api/v1/events", "POST", token, payload)

    fun updateRemoteEvent(token: String, remoteId: String, payload: JSONObject): RemoteWriteResult =
        writeJson("/api/v1/events/$remoteId", "PUT", token, payload)

    fun deleteRemoteEvent(token: String, remoteId: String): RemoteWriteResult =
        delete("/api/v1/events/$remoteId", token)

    fun createRemoteTag(token: String, payload: JSONObject): RemoteWriteResult =
        writeJson("/api/v1/tags", "POST", token, payload)

    fun updateRemoteTag(token: String, remoteId: String, payload: JSONObject): RemoteWriteResult =
        writeJson("/api/v1/tags/$remoteId", "PUT", token, payload)

    fun deleteRemoteTag(token: String, remoteId: String): RemoteWriteResult =
        delete("/api/v1/tags/$remoteId", token)

    fun uploadHar(token: String, userId: Int, fileName: String, bytes: ByteArray): ApiResult {
        return try {
            val baseUrl = fetchBaseUrl()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", userId.toString())
                .addFormDataPart(
                    "file",
                    fileName,
                    bytes.toRequestBody("application/json".toMediaType())
                )
                .build()
            val request = authenticatedRequest("$baseUrl/api/v1/analyze/", token)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return ApiResult(false, "HTTP ${response.code}: $responseBody")
                }

                val json = JSONObject(responseBody)
                if (json.optInt("code", response.code) == 202) {
                    val jobId = json.optJSONObject("body")?.optString("id").orEmpty()
                    ApiResult(true, if (jobId.isBlank()) "Added file" else "Added file. Job: $jobId")
                } else {
                    ApiResult(false, json.optString("message", "Upload failed"))
                }
            }
        } catch (error: Exception) {
            ApiResult(false, error.message ?: "Network error")
        }
    }
    fun getDiscoveryJobs(
        token: String
    ): AppResult<List<DiscoveryJob>> {
        return try {
            val request = authenticatedRequest(
                "${fetchBaseUrl()}/api/v1/analyze",
                token
            )
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return AppResult.Error(
                        "HTTP ${response.code}: $responseBody"
                    )
                }

                val json = JSONObject(responseBody)
                val code = json.optInt("code", response.code)

                if (code != 200) {
                    return AppResult.Error(
                        json.optString("message", "Cannot load discovery jobs")
                    )
                }

                val body = json.optJSONArray("body")
                val jobs = buildList {
                    if (body != null) {
                        for (index in 0 until body.length()) {
                            val item = body.optJSONObject(index) ?: continue

                            add(
                                DiscoveryJob(
                                    id = item.optString("id"),
                                    userId = if (item.isNull("userId")) {
                                        null
                                    } else {
                                        item.optInt("userId")
                                    },
                                    fileName = item.nullableString("fileName"),
                                    status = item.optString("status"),
                                    errorMessage =
                                        item.nullableString("errorMessage"),
                                    createdAt = item.optString("createdAt"),
                                    completedAt =
                                        item.nullableString("completedAt")
                                )
                            )
                        }
                    }
                }

                AppResult.Success(jobs)
            }
        } catch (error: Exception) {
            AppResult.Error(
                error.message ?: "Cannot load discovery jobs",
                error
            )
        }
    }

    fun savePortalAuthorizationCredential(sessionToken: String, portalToken: String): ApiResult {
        return try {
            val normalizedAuthorization = portalToken.trim()
                .let { if (it.startsWith("Bearer ", ignoreCase = true)) it else "Bearer $it" }
            val captureId = startPortalCredentialCapture(sessionToken)
            completePortalCredentialCapture(sessionToken, captureId, normalizedAuthorization)
        } catch (error: Exception) {
            ApiResult(false, error.message ?: "Cannot save portal credential")
        }
    }

    fun agentChatV2(token: String, message: String, confirmed: Boolean): AgentChatResult {
        return try {
            val payload = JSONObject()
                .put("message", message)
                .put("confirmed", confirmed)
                .toString()
            val request = authenticatedRequest("${fetchBaseUrl()}/api/v2/agent/chat", token)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return AgentChatResult(false, "HTTP ${response.code}: $responseBody")
                }

                val json = JSONObject(responseBody)
                val code = json.optInt("code", response.code)
                val apiMessage = json.optString("message", "unknown response")
                if (code !in 200..299) {
                    return AgentChatResult(false, apiMessage)
                }

                val body = json.optJSONObject("body")
                    ?: return AgentChatResult(false, "Agent chat response has no body")
                AgentChatResult(true, apiMessage, body.toAgentChatResponse())
            }
        } catch (error: Exception) {
            AgentChatResult(false, error.message ?: "Agent chat failed")
        }
    }

    private fun startPortalCredentialCapture(token: String): String {
        val request = authenticatedRequest("${fetchBaseUrl()}/api/v1/portal-credentials/capture/start", token)
            .post(JSONObject().toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: $responseBody")
            }
            val json = JSONObject(responseBody)
            val code = json.optInt("code", response.code)
            if (code !in 200..299) {
                throw IllegalStateException(json.optString("message", "Cannot start credential capture"))
            }
            return json.optJSONObject("body")?.optString("captureId")?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Credential capture response has no captureId")
        }
    }

    private fun completePortalCredentialCapture(
        token: String,
        captureId: String,
        authorization: String
    ): ApiResult {
        val payload = JSONObject()
            .put("authorization", authorization)
            .toString()
        val request = authenticatedRequest(
            "${fetchBaseUrl()}/api/v1/portal-credentials/capture/$captureId/complete",
            token
        )
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return ApiResult(false, "HTTP ${response.code}: $responseBody")
            }

            val json = JSONObject(responseBody)
            val code = json.optInt("code", response.code)
            val message = json.optString("message", "Portal credential saved")
            return if (code in 200..299) {
                ApiResult(true, message)
            } else {
                ApiResult(false, message)
            }
        }
    }

    private fun fetchBaseUrl(): String {
        cachedBaseUrl?.let { return it }

        val request = Request.Builder()
            .url(API_URL_SOURCE)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()?.trim().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Cannot load API URL: HTTP ${response.code}")
            }

            if (!body.startsWith("https://") || !body.contains(".trycloudflare.com")) {
                throw IllegalStateException("Invalid API URL from GitHub: $body")
            }

            return body.trimEnd('/').also {
                cachedBaseUrl = it
            }
        }
    }

    private fun writeJson(path: String, method: String, token: String, payload: JSONObject): RemoteWriteResult {
        return try {
            val request = authenticatedRequest("${fetchBaseUrl()}$path", token)
                .method(method, payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                parseWriteResponse(response.code, response.isSuccessful, response.body?.string().orEmpty())
            }
        } catch (error: Exception) {
            RemoteWriteResult(false, error.message ?: "Network error")
        }
    }

    private fun delete(path: String, token: String): RemoteWriteResult {
        return try {
            val request = authenticatedRequest("${fetchBaseUrl()}$path", token)
                .delete()
                .build()
            client.newCall(request).execute().use { response ->
                parseWriteResponse(response.code, response.isSuccessful, response.body?.string().orEmpty())
            }
        } catch (error: Exception) {
            RemoteWriteResult(false, error.message ?: "Network error")
        }
    }

    private fun authenticatedRequest(url: String, token: String): Request.Builder {
        require(token.isNotBlank()) { "Login is required" }
        return Request.Builder()
            .url(url)
            .header("X-Session-Token", token)
    }

    private fun parseWriteResponse(httpCode: Int, successful: Boolean, responseBody: String): RemoteWriteResult {
        if (!successful) return RemoteWriteResult(false, "HTTP $httpCode: $responseBody")
        val json = JSONObject(responseBody)
        val code = json.optInt("code", httpCode)
        val message = json.optString("message", "unknown response")
        if (code !in 200..299) return RemoteWriteResult(false, message)
        val body = json.optJSONObject("body")
        return RemoteWriteResult(true, message, body?.opt("id")?.toString())
    }

    private fun JSONObject.nullableString(name: String): String? =
        takeUnless { isNull(name) }?.optString(name)?.takeIf { it.isNotBlank() }

    private fun JSONObject.toAgentChatResponse(): AgentChatResponse =
        AgentChatResponse(
            answer = optString("answer"),
            toolCalls = optJSONArray("toolCalls").toAgentToolCalls(),
            needsConfirmation = optBoolean("needsConfirmation"),
            needsClarification = optBoolean("needsClarification"),
            pendingConfirmationId = nullableString("pendingConfirmationId"),
            missing = optJSONArray("missing").toStringList()
        )

    private fun org.json.JSONArray?.toAgentToolCalls(): List<AgentToolCall> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    AgentToolCall(
                        toolId = item.optInt("toolId").takeUnless { item.isNull("toolId") },
                        toolName = item.optString("toolName"),
                        category = item.optString("category"),
                        params = item.optJSONObject("params").toStringMap(),
                        upstreamStatus = item.optInt("upstreamStatus")
                            .takeUnless { item.isNull("upstreamStatus") }
                    )
                )
            }
        }
    }

    private fun org.json.JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun JSONObject?.toStringMap(): Map<String, String> {
        if (this == null) return emptyMap()
        return keys().asSequence().associateWith { key ->
            opt(key)?.toString().orEmpty()
        }
    }

}
