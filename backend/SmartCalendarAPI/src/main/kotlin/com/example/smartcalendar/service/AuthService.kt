package com.example.smartcalendar.service

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.auth.request.LoginRequest
import com.example.smartcalendar.dto.auth.request.LogoutRequest
import com.example.smartcalendar.dto.auth.request.RegisterRequest
import com.example.smartcalendar.dto.auth.response.AuthResponse
import com.example.smartcalendar.model.Account
import com.example.smartcalendar.model.Session
import com.example.smartcalendar.model.User
import com.example.smartcalendar.repository.AccountRepository
import com.example.smartcalendar.repository.SessionRepository
import com.example.smartcalendar.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val sessionRepository: SessionRepository
) {

    @Transactional
    fun register(request: RegisterRequest): ApiResponse<AuthResponse> {
        val loginName = request.loginName?.takeIf { it.isNotBlank() } ?: request.username

        if (userRepository.existsByUsername(request.username)) return ApiResponse.conflict("username already exists")
        if (userRepository.existsByEmail(request.email)) return ApiResponse.conflict("email already exists")
        if (accountRepository.existsByUsername(request.username)) return ApiResponse.conflict("account username already exists")
        if (accountRepository.existsByLoginName(loginName)) return ApiResponse.conflict("login name already exists")

        val user = userRepository.save(User(username = request.username, email = request.email, fullName = request.fullName))
        val account = accountRepository.save(
            Account(
                username = request.username,
                loginName = loginName,
                password = request.password,
                user = user
            )
        )

        return ApiResponse.created(createAuthResponse(account))
    }

    fun login(request: LoginRequest): ApiResponse<AuthResponse> {
        val account = accountRepository.findByLoginName(request.loginName)
            .orElseGet { accountRepository.findByUsername(request.loginName).orElse(null) }
            ?: return ApiResponse.unauthorized("invalid login name or password")

        if (account.password != request.password) {
            return ApiResponse.unauthorized("invalid login name or password")
        }

        return ApiResponse.success(createAuthResponse(account))
    }

    fun logout(request: LogoutRequest): ApiResponse<String> {
        val session = sessionRepository.findBySessionToken(request.sessionToken).orElse(null)
            ?: return ApiResponse.notFound("session not found")

        session.active = false
        sessionRepository.save(session)
        return ApiResponse.success("logout successfully")
    }

    fun me(sessionToken: String?): ApiResponse<AuthResponse> {
        val session = getValidSession(sessionToken)
            ?: return ApiResponse.unauthorized("invalid or expired session")
        val account = session.account
            ?: return ApiResponse.unauthorized("invalid or expired session")

        return ApiResponse.success(toAuthResponse(session, account))
    }

    fun getValidSession(sessionToken: String?): Session? {
        if (sessionToken.isNullOrBlank()) return null

        val session = sessionRepository.findBySessionToken(sessionToken).orElse(null) ?: return null
        return session.takeIf { it.active && it.expiresAt.isAfter(LocalDateTime.now()) }
    }

    private fun createAuthResponse(account: Account): AuthResponse {
        val session = sessionRepository.save(Session(sessionToken = UUID.randomUUID().toString(), account = account))
        return toAuthResponse(session, account)
    }

    private fun toAuthResponse(session: Session, account: Account): AuthResponse {
        val user = account.user ?: User()
        return AuthResponse(
            sessionToken = session.sessionToken,
            accountId = account.id,
            userId = user.id,
            username = account.username,
            loginName = account.loginName,
            email = user.email,
            fullName = user.fullName,
            expiresAt = session.expiresAt
        )
    }

}
