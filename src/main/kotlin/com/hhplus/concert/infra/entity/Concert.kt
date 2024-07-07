package com.hhplus.concert.infra.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Concert(
    title: String,
    description: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "title", nullable = false)
    var title: String = title
        protected set

    @Column(name = "description")
    var description: String = description
        protected set
}
