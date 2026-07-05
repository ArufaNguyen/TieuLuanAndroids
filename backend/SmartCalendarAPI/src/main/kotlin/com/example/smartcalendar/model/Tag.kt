package com.example.smartcalendar.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "tags")
class Tag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @Column(nullable = false, length = 100)
    var name: String = "",

    @Column(length = 50)
    var color: String? = null,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @JsonIgnore
    @OneToMany(mappedBy = "tag")
    var events: MutableList<Event> = mutableListOf()
)
