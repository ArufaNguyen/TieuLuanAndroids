package com.example.smartcalendar.repository

import com.example.smartcalendar.model.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface AccountRepository : JpaRepository<Account, Int> {

    fun existsByUsername(username: String): Boolean

    fun existsByLoginName(loginName: String): Boolean

    fun existsByUserId(userId: Int): Boolean

    fun existsByUsernameAndIdNot(username: String, id: Int): Boolean

    fun existsByLoginNameAndIdNot(loginName: String, id: Int): Boolean

    fun existsByUserIdAndIdNot(userId: Int, id: Int): Boolean

    fun findByUsername(username: String): Optional<Account>

    fun findByLoginName(loginName: String): Optional<Account>

    @Query(
        """
        SELECT a FROM Account a
        WHERE (:keyword IS NULL
            OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(a.loginName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(a.user.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(a.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY a.username ASC
        """
    )
    fun searchAccounts(@Param("keyword") keyword: String?): List<Account>
}
