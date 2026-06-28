package com.example.smartcalendar.service

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.tag.request.CreateTagRequest
import com.example.smartcalendar.dto.tag.request.UpdateTagRequest
import com.example.smartcalendar.dto.tag.response.TagResponse
import com.example.smartcalendar.dto.tag.response.TagResponseDetail
import com.example.smartcalendar.dto.user.response.UserResponse
import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.model.Tag
import com.example.smartcalendar.repository.TagRepository
import com.example.smartcalendar.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class TagService(
    private val tagRepository: TagRepository,
    private val userRepository: UserRepository
) {

    fun getTags(keyword: String?, userId: Int?): ApiResponse<List<TagResponse>> {
        val tags = tagRepository.searchTags(keyword?.takeIf { it.isNotBlank() }, userId)
        return ApiResponse.success(tags.map(::toResponse))
    }

    fun getTagById(id: Int, activeUserId: Int): ApiResponse<TagResponseDetail> {
        val tag = tagRepository.findById(id).orElse(null) ?: return ApiResponse.notFound("tag not found")
        requireOwner(tag, activeUserId)
        return ApiResponse.success(toDetail(tag))
    }

    fun createTag(request: CreateTagRequest, activeUserId: Int): ApiResponse<TagResponseDetail> {
        val user = userRepository.findById(activeUserId).orElse(null)
            ?: return ApiResponse.notFound("user not found")
        val tag = tagRepository.save(Tag(name = request.name, color = request.color, user = user))
        return ApiResponse.created(toDetail(tag))
    }

    fun updateTag(id: Int, request: UpdateTagRequest, activeUserId: Int): ApiResponse<TagResponseDetail> {
        val tag = tagRepository.findById(id).orElse(null) ?: return ApiResponse.notFound("tag not found")
        requireOwner(tag, activeUserId)
        val user = userRepository.findById(activeUserId).orElse(null)
            ?: return ApiResponse.notFound("user not found")

        tag.name = request.name
        tag.color = request.color
        tag.user = user
        return ApiResponse.success(toDetail(tagRepository.save(tag)))
    }

    fun deleteTag(id: Int, activeUserId: Int): ApiResponse<String> {
        val tag = tagRepository.findById(id).orElse(null) ?: return ApiResponse.notFound("tag not found")
        requireOwner(tag, activeUserId)
        return try {
            tagRepository.delete(tag)
            ApiResponse.success("tag deleted successfully")
        } catch (_: DataIntegrityViolationException) {
            ApiResponse.conflict("tag cannot be deleted because it is used by events")
        }
    }

    private fun requireOwner(tag: Tag, activeUserId: Int) {
        if (tag.user?.id != activeUserId) {
            throw ApiException(403, "tag does not belong to the active session")
        }
    }

    private fun toResponse(tag: Tag) = TagResponse(tag.id, tag.name, tag.color, tag.user?.id)

    private fun toDetail(tag: Tag) = TagResponseDetail(
        tag.id,
        tag.name,
        tag.color,
        tag.user?.let { UserResponse(it.id, it.username, it.email, it.fullName) }
    )
}
