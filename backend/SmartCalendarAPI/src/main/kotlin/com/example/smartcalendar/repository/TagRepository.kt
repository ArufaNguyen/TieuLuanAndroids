package com.example.smartcalendar.repository

import com.example.smartcalendar.model.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TagRepository : JpaRepository<Tag, Int> {
    fun findByNameContainingIgnoreCase(name: String): List<Tag>

    @Query(
        """
        SELECT t FROM Tag t
        WHERE (:keyword IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:userId IS NULL OR t.user.id = :userId)
        ORDER BY t.name ASC
        """
    )
    fun searchTags(
        @Param("keyword") keyword: String?,
        @Param("userId") userId: Int?
    ): List<Tag>
}
