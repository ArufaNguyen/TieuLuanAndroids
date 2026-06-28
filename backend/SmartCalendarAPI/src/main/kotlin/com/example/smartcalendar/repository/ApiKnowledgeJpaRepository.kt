package com.example.smartcalendar.repository

import com.example.smartcalendar.model.ApiKnowledgeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ApiKnowledgeJpaRepository : JpaRepository<ApiKnowledgeEntity, Int> {
    fun findAllByUserIdOrderByUpdatedAtDesc(userId: Int): List<ApiKnowledgeEntity>
    fun findAllByUserIsNullOrderByUpdatedAtDesc(): List<ApiKnowledgeEntity>
    fun findByUserIdAndNaturalKey(userId: Int, naturalKey: String): ApiKnowledgeEntity?
    fun findByUserIsNullAndNaturalKey(naturalKey: String): ApiKnowledgeEntity?
    fun findFirstByUserIdAndToolNameOrderByUpdatedAtDescIdDesc(userId: Int, toolName: String): ApiKnowledgeEntity?
    fun findFirstByUserIsNullAndToolNameOrderByUpdatedAtDescIdDesc(toolName: String): ApiKnowledgeEntity?
}
