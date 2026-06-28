package com.example.tieuluanandroids.data.api

import org.json.JSONArray

/** Chuyển mảng JSON từ API thành các model Kotlin thuần của tầng data. */
internal object RemoteJsonParser {
    fun parseEvents(jsonArray: JSONArray?): List<RemoteEvent> {
        if (jsonArray == null) return emptyList()

        val events = mutableListOf<RemoteEvent>()
        for (index in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(index) ?: continue
            val tag = item.optJSONObject("tag")
            val user = item.optJSONObject("user")

            events += RemoteEvent(
                id = item.opt("id")?.toString().orEmpty(),
                title = item.optString("title", ""),
                description = item.optString("description").takeIf { it.isNotBlank() },
                startTime = item.optString("startTime", ""),
                endTime = item.optString("endTime", ""),
                tagId = (tag?.opt("id") ?: item.opt("tagId")).asNullableString(),
                tagName = tag?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: item.optString("tagName").takeIf { it.isNotBlank() }
                    ?: "-",
                ownerId = (user?.opt("id") ?: item.opt("userId")).asNullableString(),
                ownerName = user?.optString("username")?.takeIf { it.isNotBlank() } ?: "-"
            )
        }
        return events
    }

    fun parseTags(jsonArray: JSONArray?): List<RemoteTag> {
        if (jsonArray == null) return emptyList()

        val tags = mutableListOf<RemoteTag>()
        for (index in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(index) ?: continue
            tags += RemoteTag(
                id = item.opt("id")?.toString().orEmpty(),
                name = item.optString("name", ""),
                color = item.optString("color").takeIf { it.isNotBlank() },
                ownerId = item.opt("userId").asNullableString()
            )
        }
        return tags
    }

    private fun Any?.asNullableString(): String? {
        val text = this?.toString()
        return text?.takeIf { it.isNotBlank() && it != "null" }
    }
}
