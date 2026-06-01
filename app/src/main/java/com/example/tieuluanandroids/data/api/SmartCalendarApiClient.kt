package com.example.tieuluanandroids.data.api

import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.CookieManager
import java.net.CookiePolicy

object SmartCalendarApiClient {

    private const val API_URL_SOURCE =
        "https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(cookieManager))
        .build()

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    var devMode: Boolean = false
        private set

    fun login(username: String, password: String): ApiResult {
        return try {
            val baseUrl = fetchBaseUrl()
            val payload = JSONObject()
                .put("userId", JSONObject.NULL)
                .put("username", username)
                .put("email", JSONObject.NULL)
                .put("password", password)
                .toString()

            val request = Request.Builder()
                .url("$baseUrl/api/session/login")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return ApiResult(false, "HTTP ${response.code}: $responseBody")
                }

                val json = JSONObject(responseBody)
                val code = json.optInt("code", response.code)
                val message = json.optString("message", "unknown response")

                if (code == 200) {
                    devMode = false
                    ApiResult(true, "Login success")
                } else {
                    ApiResult(false, message)
                }
            }
        } catch (error: Exception) {
            ApiResult(false, error.message ?: "Network error")
        }
    }

    fun enableDevMode() {
        devMode = true
    }

    fun checkBackendDevMode(): ApiResult {
        return try {
            val baseUrl = fetchBaseUrl()
            val request = Request.Builder()
                .url("$baseUrl/api/session/dev-mode")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return ApiResult(false, "HTTP ${response.code}: $responseBody")
                }

                val json = JSONObject(responseBody)
                val enabled = json.optJSONObject("body")?.optBoolean("enabled") ?: false
                if (enabled) {
                    ApiResult(true, "Backend dev mode enabled")
                } else {
                    ApiResult(false, "Backend DEV_MODE=false. Set DEV_MODE=true in backend/SmartCalendarAPI/.env and restart backend.")
                }
            }
        } catch (error: Exception) {
            ApiResult(false, error.message ?: "Network error")
        }
    }

    fun getEvents(): EventsResult {
        return try {
            val baseUrl = fetchBaseUrl()
            val request = Request.Builder()
                .url("$baseUrl/api/events")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return EventsResult(false, "HTTP ${response.code}: $responseBody", emptyList())
                }

                val json = JSONObject(responseBody)
                val code = json.optInt("code", response.code)
                val message = json.optString("message", "unknown response")
                if (code != 200) {
                    return EventsResult(false, message, emptyList())
                }

                EventsResult(true, message, parseEvents(json.optJSONArray("body")))
            }
        } catch (error: Exception) {
            EventsResult(false, error.message ?: "Network error", emptyList())
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

    private fun parseEvents(eventsJson: JSONArray?): List<EventItem> {
        if (eventsJson == null) {
            return emptyList()
        }

        return buildList {
            for (index in 0 until eventsJson.length()) {
                val item = eventsJson.optJSONObject(index) ?: continue
                val tag = item.optJSONObject("tag")
                val user = item.optJSONObject("user")

                add(
                    EventItem(
                        title = item.optString("title", ""),
                        startTime = item.optString("startTime", ""),
                        endTime = item.optString("endTime", ""),
                        tagName = tag?.optString("name")?.takeIf { it.isNotBlank() } ?: "-",
                        ownerName = user?.optString("username")?.takeIf { it.isNotBlank() } ?: "-"
                    )
                )
            }
        }
    }
}

data class ApiResult(
    val success: Boolean,
    val message: String
)

data class EventsResult(
    val success: Boolean,
    val message: String,
    val events: List<EventItem>
)

data class EventItem(
    val title: String,
    val startTime: String,
    val endTime: String,
    val tagName: String,
    val ownerName: String
)
