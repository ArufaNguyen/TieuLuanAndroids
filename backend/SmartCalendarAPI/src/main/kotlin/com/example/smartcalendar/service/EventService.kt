package com.example.smartcalendar.service

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.event.request.CreateEventRequest
import com.example.smartcalendar.dto.event.request.UpdateEventRequest
import com.example.smartcalendar.dto.event.response.EventResponse
import com.example.smartcalendar.dto.event.response.EventResponseDetail
import com.example.smartcalendar.dto.tag.response.TagResponse
import com.example.smartcalendar.dto.user.response.UserResponse
import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.model.Event
import com.example.smartcalendar.repository.EventRepository
import com.example.smartcalendar.repository.TagRepository
import com.example.smartcalendar.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val tagRepository: TagRepository,
    private val userRepository: UserRepository
) {

    fun getEvents(
        keyword: String?,
        tagId: Int?,
        userId: Int?,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): ApiResponse<List<EventResponse>> {
        val events = eventRepository.searchEvents(keyword?.takeIf { it.isNotBlank() }, tagId, userId, from, to)
        return ApiResponse.success(events.map(::toResponse))
    }

    fun getEventById(id: Int, activeUserId: Int): ApiResponse<EventResponseDetail> {
        val event = eventRepository.findById(id).orElse(null) ?: return ApiResponse.notFound("event not found")
        requireOwner(event, activeUserId)
        return ApiResponse.success(toDetail(event))
    }

    fun createEvent(request: CreateEventRequest, activeUserId: Int): ApiResponse<EventResponseDetail> {
        val startTime = request.startTime ?: return ApiResponse.badRequest("startTime is required")
        val endTime = request.endTime ?: return ApiResponse.badRequest("endTime is required")
        if (!endTime.isAfter(startTime)) return ApiResponse.badRequest("endTime must be after startTime")

        val tag = request.tagId?.let {
            tagRepository.findById(it).orElse(null) ?: return ApiResponse.notFound("tag not found")
        }
        val user = userRepository.findById(activeUserId).orElse(null)
            ?: return ApiResponse.notFound("user not found")
        if (tag != null && tag.user?.id != activeUserId) {
            throw ApiException(403, "tag does not belong to the active session")
        }

        val event = eventRepository.save(
            Event(
                title = request.title,
                description = request.description,
                startTime = startTime,
                endTime = endTime,
                tag = tag,
                user = user
            )
        )
        return ApiResponse.created(toDetail(event))
    }

    fun updateEvent(id: Int, request: UpdateEventRequest, activeUserId: Int): ApiResponse<EventResponseDetail> {
        val event = eventRepository.findById(id).orElse(null) ?: return ApiResponse.notFound("event not found")
        requireOwner(event, activeUserId)
        if (!request.endTime.isAfter(request.startTime)) return ApiResponse.badRequest("endTime must be after startTime")

        val tag = request.tagId?.let {
            tagRepository.findById(it).orElse(null) ?: return ApiResponse.notFound("tag not found")
        }
        val user = userRepository.findById(activeUserId).orElse(null)
            ?: return ApiResponse.notFound("user not found")
        if (tag != null && tag.user?.id != activeUserId) {
            throw ApiException(403, "tag does not belong to the active session")
        }

        event.title = request.title
        event.description = request.description
        event.startTime = request.startTime
        event.endTime = request.endTime
        event.tag = tag
        event.user = user
        return ApiResponse.success(toDetail(eventRepository.save(event)))
    }

    fun deleteEvent(id: Int, activeUserId: Int): ApiResponse<String> {
        val event = eventRepository.findById(id).orElse(null) ?: return ApiResponse.notFound("event not found")
        requireOwner(event, activeUserId)
        eventRepository.delete(event)
        return ApiResponse.success("event deleted successfully")
    }

    private fun requireOwner(event: Event, activeUserId: Int) {
        if (event.user?.id != activeUserId) {
            throw ApiException(403, "event does not belong to the active session")
        }
    }

    private fun toResponse(event: Event) = EventResponse(
        id = event.id,
        title = event.title,
        startTime = event.startTime,
        endTime = event.endTime,
        tagId = event.tag?.id,
        tagName = event.tag?.name,
        userId = event.user?.id
    )

    private fun toDetail(event: Event) = EventResponseDetail(
        id = event.id,
        title = event.title,
        description = event.description,
        startTime = event.startTime,
        endTime = event.endTime,
        tag = event.tag?.let { TagResponse(it.id, it.name, it.color, it.user?.id) },
        user = event.user?.let { UserResponse(it.id, it.username, it.email, it.fullName) }
    )
}
