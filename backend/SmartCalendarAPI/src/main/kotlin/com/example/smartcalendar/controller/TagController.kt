package com.example.smartcalendar.controller

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.tag.request.CreateTagRequest
import com.example.smartcalendar.dto.tag.request.UpdateTagRequest
import com.example.smartcalendar.dto.tag.response.TagResponse
import com.example.smartcalendar.dto.tag.response.TagResponseDetail
import com.example.smartcalendar.service.TagService
import com.example.smartcalendar.service.SessionAuthorizationService
import jakarta.validation.Valid
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

@RestController
@RequestMapping("/api/v1/tags")
class TagController(
    private val tagService: TagService,
    private val authorization: SessionAuthorizationService
) {

    @GetMapping
    fun getTags(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) userId: Int?
    ): ApiResponse<List<TagResponse>> =
        tagService.getTags(keyword, authorization.requireUser(sessionToken, userId).id)

    @GetMapping("/{id}")
    fun getTagById(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable id: Int
    ): ApiResponse<TagResponseDetail> = tagService.getTagById(id, authorization.requireUser(sessionToken).id)

    @PostMapping
    fun createTag(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @Valid @RequestBody request: CreateTagRequest
    ): ApiResponse<TagResponseDetail> =
        tagService.createTag(request, authorization.requireUser(sessionToken, request.userId).id)

    @PutMapping("/{id}")
    fun updateTag(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateTagRequest
    ): ApiResponse<TagResponseDetail> =
        tagService.updateTag(id, request, authorization.requireUser(sessionToken, request.userId).id)

    @DeleteMapping("/{id}")
    fun deleteTag(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable id: Int
    ): ApiResponse<String> = tagService.deleteTag(id, authorization.requireUser(sessionToken).id)
}
