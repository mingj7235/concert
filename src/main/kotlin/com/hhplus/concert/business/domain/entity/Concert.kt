package com.hhplus.concert.business.domain.entity

import com.hhplus.concert.common.type.ConcertStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    @Enumerated(EnumType.STRING)
    var concertStatus: ConcertStatus = concertStatus
        protected set

    fun updateStatus(status: ConcertStatus) {
        this.concertStatus = status
    }
}
