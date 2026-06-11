package com.example.smartcalendar.controller

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.user.request.CreateUserRequest
import com.example.smartcalendar.dto.user.request.UpdateUserRequest
import com.example.smartcalendar.dto.user.response.UserResponse
import com.example.smartcalendar.dto.user.response.UserResponseDetail
import com.example.smartcalendar.service.UserService
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
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    @GetMapping
    fun getUsers(@RequestParam(required = false) keyword: String?): ApiResponse<List<UserResponse>> =
        userService.getUsers(keyword)

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Int): ApiResponse<UserResponseDetail> = userService.getUserById(id)

    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ApiResponse<UserResponseDetail> =
        userService.createUser(request)

    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateUserRequest
    ): ApiResponse<UserResponseDetail> = userService.updateUser(id, request)

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Int): ApiResponse<String> = userService.deleteUser(id)
}
