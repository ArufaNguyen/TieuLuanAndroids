package com.example.smartcalendar.controller

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.tag.request.CreateTagRequest
import com.example.smartcalendar.dto.tag.request.UpdateTagRequest
import com.example.smartcalendar.dto.tag.response.TagResponse
import com.example.smartcalendar.dto.tag.response.TagResponseDetail
import com.example.smartcalendar.service.TagService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tags")
class TagController(private val tagService: TagService) {

    @GetMapping
    fun getTags(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) userId: Int?
    ): ApiResponse<List<TagResponse>> = tagService.getTags(keyword, userId)

    @GetMapping("/{id}")
    fun getTagById(@PathVariable id: Int): ApiResponse<TagResponseDetail> = tagService.getTagById(id)

    @PostMapping
    fun createTag(@Valid @RequestBody request: CreateTagRequest): ApiResponse<TagResponseDetail> =
        tagService.createTag(request)

    @PutMapping("/{id}")
    fun updateTag(
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateTagRequest
    ): ApiResponse<TagResponseDetail> = tagService.updateTag(id, request)

    @DeleteMapping("/{id}")
    fun deleteTag(@PathVariable id: Int): ApiResponse<String> = tagService.deleteTag(id)
}
