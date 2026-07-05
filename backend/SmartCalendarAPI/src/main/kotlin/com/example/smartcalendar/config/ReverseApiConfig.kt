package com.example.smartcalendar.config

import com.example.smartcalendar.service.ReverseApiProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.unit.DataSize

@Configuration
class ReverseApiConfig {
    @Bean
    fun reverseApiProperties(
        @Value("\${app.reverse-api.max-file-size:30MB}") maxFileSize: DataSize
    ) = ReverseApiProperties(maxFileSize)
}
