package com.example.smartcalendar.repository

import com.example.smartcalendar.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Int> {

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    fun existsByUsernameAndIdNot(username: String, id: Int): Boolean

    fun existsByEmailAndIdNot(email: String, id: Int): Boolean

    @Query(
        """
        SELECT u FROM User u
        WHERE (:keyword IS NULL
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY u.username ASC
        """
    )
    fun searchUsers(@Param("keyword") keyword: String?): List<User>
}
