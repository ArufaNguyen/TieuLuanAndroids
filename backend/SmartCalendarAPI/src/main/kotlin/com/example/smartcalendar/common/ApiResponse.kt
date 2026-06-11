package com.example.smartcalendar.common

open class ApiResponse<T>(
    val code: Int,
    val message: String,
    val body: T?
) {
    companion object {
        fun <T> success(body: T?): ApiResponse<T> = ApiResponse(200, "success", body)

        fun <T> created(body: T?): ApiResponse<T> = ApiResponse(201, "created", body)

        fun <T> badRequest(message: String): ApiResponse<T> = ApiResponse(400, message, null)

        fun <T> unauthorized(message: String): ApiResponse<T> = ApiResponse(401, message, null)

        fun <T> notFound(message: String): ApiResponse<T> = ApiResponse(404, message, null)

        fun <T> conflict(message: String): ApiResponse<T> = ApiResponse(409, message, null)

        fun <T> internalError(message: String): ApiResponse<T> = ApiResponse(500, message, null)
    }
}
