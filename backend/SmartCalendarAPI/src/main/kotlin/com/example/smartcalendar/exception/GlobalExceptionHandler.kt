package com.example.smartcalendar.exception

import com.example.smartcalendar.common.ApiResponse
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApiException(exception: ApiException): ApiResponse<Nothing> {
        return ApiResponse(exception.code, exception.message, null)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ApiResponse<Nothing> {
        val message = exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: "bad request"
        return ApiResponse.badRequest(message)
    }

    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class
    )
    fun handleBadRequest(exception: Exception): ApiResponse<Nothing> {
        return ApiResponse.badRequest(exception.message ?: "bad request")
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(exception: NoResourceFoundException): ApiResponse<Nothing> {
        return ApiResponse.notFound("resource not found")
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ApiResponse<Nothing> {
        return ApiResponse.internalError(exception.message ?: "internal server error")
    }
}
