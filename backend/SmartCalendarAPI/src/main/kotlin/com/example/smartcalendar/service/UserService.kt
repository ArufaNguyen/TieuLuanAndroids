package com.example.smartcalendar.service

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.user.request.CreateUserRequest
import com.example.smartcalendar.dto.user.request.UpdateUserRequest
import com.example.smartcalendar.dto.user.response.UserResponse
import com.example.smartcalendar.dto.user.response.UserResponseDetail
import com.example.smartcalendar.model.User
import com.example.smartcalendar.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: UserRepository) {

    fun getUsers(keyword: String?): ApiResponse<List<UserResponse>> {
        val users = userRepository.searchUsers(keyword?.takeIf { it.isNotBlank() })
        return ApiResponse.success(users.map(::toResponse))
    }

    fun getUserById(id: Int): ApiResponse<UserResponseDetail> {
        val user = userRepository.findById(id).orElse(null) ?: return ApiResponse.notFound("user not found")
        return ApiResponse.success(toDetail(user))
    }

    fun createUser(request: CreateUserRequest): ApiResponse<UserResponseDetail> {
        if (userRepository.existsByUsername(request.username)) return ApiResponse.conflict("username already exists")
        if (userRepository.existsByEmail(request.email)) return ApiResponse.conflict("email already exists")

        val user = userRepository.save(User(username = request.username, email = request.email, fullName = request.fullName))
        return ApiResponse.created(toDetail(user))
    }

    fun updateUser(id: Int, request: UpdateUserRequest): ApiResponse<UserResponseDetail> {
        val user = userRepository.findById(id).orElse(null) ?: return ApiResponse.notFound("user not found")
        if (userRepository.existsByUsernameAndIdNot(request.username, id)) return ApiResponse.conflict("username already exists")
        if (userRepository.existsByEmailAndIdNot(request.email, id)) return ApiResponse.conflict("email already exists")

        user.username = request.username
        user.email = request.email
        user.fullName = request.fullName
        return ApiResponse.success(toDetail(userRepository.save(user)))
    }

    fun deleteUser(id: Int): ApiResponse<String> {
        if (!userRepository.existsById(id)) return ApiResponse.notFound("user not found")
        return try {
            userRepository.deleteById(id)
            ApiResponse.success("user deleted successfully")
        } catch (_: DataIntegrityViolationException) {
            ApiResponse.conflict("user cannot be deleted because it is used by another record")
        }
    }

    private fun toResponse(user: User) = UserResponse(user.id, user.username, user.email, user.fullName)

    private fun toDetail(user: User) = UserResponseDetail(user.id, user.username, user.email, user.fullName)
}
