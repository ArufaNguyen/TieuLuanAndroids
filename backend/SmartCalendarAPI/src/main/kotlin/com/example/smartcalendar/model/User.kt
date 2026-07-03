package com.example.smartcalendar.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @Column(nullable = false, unique = true, length = 100)
    var username: String = "",

    @Column(nullable = false, unique = true, length = 255)
    var email: String = "",

    @Column(name = "full_name", length = 255)
    var fullName: String? = null,

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    var tags: MutableList<Tag> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    var events: MutableList<Event> = mutableListOf(),

    @JsonIgnore
    @OneToOne(mappedBy = "user")
    var account: Account? = null
)
