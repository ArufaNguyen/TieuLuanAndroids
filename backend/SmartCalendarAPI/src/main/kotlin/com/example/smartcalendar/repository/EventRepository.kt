package com.example.smartcalendar.repository

import com.example.smartcalendar.model.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface EventRepository : JpaRepository<Event, Int> {

    fun findByTitleContainingIgnoreCase(title: String): List<Event>

    fun findByTagId(tagId: Int): List<Event>

    fun findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
        from: LocalDateTime,
        to: LocalDateTime
    ): List<Event>

    @Query(
        """
        SELECT e FROM Event e
        WHERE (:keyword IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:tagId IS NULL OR e.tag.id = :tagId)
          AND (:userId IS NULL OR e.user.id = :userId)
          AND (:from IS NULL OR e.startTime >= :from)
          AND (:to IS NULL OR e.endTime <= :to)
        ORDER BY e.startTime ASC
        """
    )
    fun searchEvents(
        @Param("keyword") keyword: String?,
        @Param("tagId") tagId: Int?,
        @Param("userId") userId: Int?,
        @Param("from") from: LocalDateTime?,
        @Param("to") to: LocalDateTime?
    ): List<Event>
}
