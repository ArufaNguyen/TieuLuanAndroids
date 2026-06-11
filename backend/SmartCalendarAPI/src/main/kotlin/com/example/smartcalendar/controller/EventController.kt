package com.example.smartcalendar.controller

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.event.request.CreateEventRequest
import com.example.smartcalendar.dto.event.request.UpdateEventRequest
import com.example.smartcalendar.dto.event.response.EventResponse
import com.example.smartcalendar.dto.event.response.EventResponseDetail
import com.example.smartcalendar.service.EventService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/events")
class EventController(private val eventService: EventService) {

    @GetMapping
    fun getEvents(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) tagId: Int?,
        @RequestParam(required = false) userId: Int?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?
    ): ApiResponse<List<EventResponse>> = eventService.getEvents(keyword, tagId, userId, from, to)

    @GetMapping("/{id}")
    fun getEventById(@PathVariable id: Int): ApiResponse<EventResponseDetail> = eventService.getEventById(id)

    @PostMapping
    fun createEvent(@Valid @RequestBody request: CreateEventRequest): ApiResponse<EventResponseDetail> =
        eventService.createEvent(request)

    @PutMapping("/{id}")
    fun updateEvent(
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateEventRequest
    ): ApiResponse<EventResponseDetail> = eventService.updateEvent(id, request)

    @DeleteMapping("/{id}")
    fun deleteEvent(@PathVariable id: Int): ApiResponse<String> = eventService.deleteEvent(id)
}
