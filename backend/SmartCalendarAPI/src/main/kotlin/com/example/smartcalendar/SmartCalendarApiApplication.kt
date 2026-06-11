package com.example.smartcalendar

import com.example.smartcalendar.config.DotenvConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SmartCalendarApiApplication

fun main(args: Array<String>) {
    DotenvConfig.load()
    runApplication<SmartCalendarApiApplication>(*args)
}
