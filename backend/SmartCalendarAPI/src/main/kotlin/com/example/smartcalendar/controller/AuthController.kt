package com.example.smartcalendar.controller

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.auth.request.LoginRequest
import com.example.smartcalendar.dto.auth.request.LogoutRequest
import com.example.smartcalendar.dto.auth.request.RegisterRequest
import com.example.smartcalendar.dto.auth.response.AuthResponse
import com.example.smartcalendar.service.AuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ApiResponse<AuthResponse> =
        authService.register(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ApiResponse<AuthResponse> =
        authService.login(request)

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ApiResponse<String> =
        authService.logout(request)

    @GetMapping("/me")
    fun me(@RequestHeader("X-Session-Token", required = false) sessionToken: String?): ApiResponse<AuthResponse> =
        authService.me(sessionToken)
}
