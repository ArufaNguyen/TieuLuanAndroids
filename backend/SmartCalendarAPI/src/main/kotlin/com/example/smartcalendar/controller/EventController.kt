package com.example.smartcalendar.controller

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.event.request.CreateEventRequest
import com.example.smartcalendar.dto.event.request.UpdateEventRequest
import com.example.smartcalendar.dto.event.response.EventResponse
import com.example.smartcalendar.dto.event.response.EventResponseDetail
import com.example.smartcalendar.service.EventService
import com.example.smartcalendar.service.SessionAuthorizationService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val eventService: EventService,
    private val authorization: SessionAuthorizationService
) {

    @GetMapping
    fun getEvents(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) tagId: Int?,
        @RequestParam(required = false) userId: Int?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?
    ): ApiResponse<List<EventResponse>> {
        val activeUserId = authorization.requireUser(sessionToken, userId).id
        return eventService.getEvents(keyword, tagId, activeUserId, from, to)
    }

    @GetMapping("/{id}")
    fun getEventById(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable id: Int
    ): ApiResponse<EventResponseDetail> = eventService.getEventById(id, authorization.requireUser(sessionToken).id)

    @PostMapping
    fun createEvent(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @Valid @RequestBody request: CreateEventRequest
    ): ApiResponse<EventResponseDetail> =
        eventService.createEvent(request, authorization.requireUser(sessionToken, request.userId).id)

    @PutMapping("/{id}")
    fun updateEvent(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateEventRequest
    ): ApiResponse<EventResponseDetail> =
        eventService.updateEvent(id, request, authorization.requireUser(sessionToken, request.userId).id)

    @DeleteMapping("/{id}")
    fun deleteEvent(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable id: Int
    ): ApiResponse<String> = eventService.deleteEvent(id, authorization.requireUser(sessionToken).id)
}
