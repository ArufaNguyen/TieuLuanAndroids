package com.example.smartcalendar.portaldiscovery.agents

import com.example.smartcalendar.portaldiscovery.CalendarMapping
import com.example.smartcalendar.portaldiscovery.ToolDefinition
import com.example.smartcalendar.portaldiscovery.EndpointCategory

class BedivereToolDesignerAgent {
    fun design(endpointId: String, category: EndpointCategory, mapping: CalendarMapping? = null) =
        ToolDefinition(toolName(category), endpointId, category, mapping)

    private fun toolName(category: EndpointCategory): String = when (category) {
        EndpointCategory.LOGIN -> "login"
        EndpointCategory.SCHEDULE -> "get_student_schedule"
        EndpointCategory.REGISTERED_COURSES -> "get_registered_courses"
        EndpointCategory.AVAILABLE_COURSES -> "get_available_courses"
        EndpointCategory.RETAKE_COURSES -> "get_retake_courses"
        EndpointCategory.NOTIFICATION -> "get_notifications"
        EndpointCategory.SEMESTER -> "get_semesters"
        else -> error("No read tool is allowed for $category")
    }
}
