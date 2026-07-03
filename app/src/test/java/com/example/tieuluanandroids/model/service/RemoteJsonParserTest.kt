package com.example.tieuluanandroids.model.service

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteJsonParserTest {
    @Test
    fun `parse events reads nested tag and user`() {
        val json = JSONArray(
            """
            [
              {
                "id": 7,
                "title": "Kotlin",
                "startTime": "2026-01-01T08:00:00",
                "endTime": "2026-01-01T09:00:00",
                "tag": {"id": 3, "name": "Study"},
                "user": {"id": 1, "username": "arufa"}
              }
            ]
            """.trimIndent()
        )

        val event = RemoteJsonParser.parseEvents(json).single()

        assertEquals("7", event.id)
        assertEquals("3", event.tagId)
        assertEquals("1", event.ownerId)
        assertEquals("arufa", event.ownerName)
    }

    @Test
    fun `parse tags keeps null owner as null`() {
        val json = JSONArray("""[{"id": 4, "name": "Local", "userId": null}]""")

        val tag = RemoteJsonParser.parseTags(json).single()

        assertEquals("4", tag.id)
        assertNull(tag.ownerId)
    }
}
