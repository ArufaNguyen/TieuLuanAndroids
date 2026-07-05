package com.example.smartcalendar.controller

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.reverseapi.ApiKnowledgeResponse
import com.example.smartcalendar.dto.reverseapi.UpsertGlobalApiKnowledgePresetRequest
import com.example.smartcalendar.dto.reverseapi.UpsertGlobalApiKnowledgeRequest
import com.example.smartcalendar.service.ApiKnowledgeService
import com.example.smartcalendar.service.SessionAuthorizationService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/api-knowledge")
class ApiKnowledgeController(
    private val service: ApiKnowledgeService,
    private val authorization: SessionAuthorizationService
) {

    @GetMapping
    fun getAll(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestParam(name = "user_id", required = false) userIdSnake: Int?,
        @RequestParam(name = "userId", required = false) userIdCamel: Int?
    ): ApiResponse<List<ApiKnowledgeResponse>> {
        val userId = authorization.requireUser(sessionToken, userIdSnake ?: userIdCamel).id
        return ApiResponse.success(service.getAll(userId))
    }

    @GetMapping("/me")
    fun getMine(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?
    ): ApiResponse<List<ApiKnowledgeResponse>> {
        val userId = authorization.requireUser(sessionToken).id
        return ApiResponse.success(service.getAll(userId))
    }

    @GetMapping("/global")
    fun getGlobal(): ApiResponse<List<ApiKnowledgeResponse>> =
        ApiResponse.success(service.getGlobal())

    @PostMapping("/global")
    fun upsertGlobal(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestBody request: UpsertGlobalApiKnowledgeRequest
    ): ApiResponse<ApiKnowledgeResponse> {
        authorization.requireUser(sessionToken)
        return ApiResponse.success(service.upsertGlobal(request))
    }

    @PostMapping("/global/preset")
    fun upsertGlobalPreset(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestBody request: UpsertGlobalApiKnowledgePresetRequest
    ): ApiResponse<ApiKnowledgeResponse> {
        authorization.requireUser(sessionToken)
        return ApiResponse.success(service.upsertGlobalPreset(request))
    }

    @PostMapping("/{id}/copy-to-global")
    fun copyToGlobal(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable id: Int
    ): ApiResponse<ApiKnowledgeResponse> {
        val source = service.get(id)
        source.userId?.let { authorization.requireUser(sessionToken, it) }
            ?: authorization.requireUser(sessionToken)
        return ApiResponse.success(service.copyToGlobal(id))
    }

    @GetMapping("/{id}")
    fun get(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable id: Int
    ): ApiResponse<ApiKnowledgeResponse> {
        val result = service.get(id)
        result.userId?.let { authorization.requireUser(sessionToken, it) }
        return ApiResponse.success(result)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable id: Int
    ): ApiResponse<String> {
        val target = service.get(id)
        target.userId?.let { authorization.requireUser(sessionToken, it) }
            ?: authorization.requireUser(sessionToken)
        return ApiResponse.success(service.delete(id))
    }
}
