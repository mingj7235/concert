package com.hhplus.concert.infrastructure.entity

import com.hhplus.concert.common.type.ConcertStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Concert(
    title: String,
    description: String,
    concertStatus: ConcertStatus,
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

    @Column(name = "status")
    var concertStatus: ConcertStatus = concertStatus
        protected set
}
