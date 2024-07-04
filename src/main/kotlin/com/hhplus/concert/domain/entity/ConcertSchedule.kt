package com.hhplus.concert.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

@Entity
class ConcertSchedule(
    concert: Concert,
    concertAt: LocalDateTime,
    reservationAvailableAt: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne
    @JoinColumn(name = "concert_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var concert: Concert = concert
        protected set

    @Column(name = "concert_at", nullable = false)
    var concertAt: LocalDateTime = concertAt
        protected set

    @Column(name = "reservation_available_at", nullable = false)
    var reservationAvailableAt: LocalDateTime = reservationAvailableAt
        protected set
}
