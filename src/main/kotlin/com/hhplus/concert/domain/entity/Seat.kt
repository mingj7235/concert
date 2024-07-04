package com.hhplus.concert.domain.entity

import com.hhplus.concert.common.type.SeatStatus
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Seat(
    concertSchedule: ConcertSchedule,
    seatNumber: Int,
    seatStatus: SeatStatus,
    seatPrice: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne
    @JoinColumn(name = "concert_schedule_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var concertSchedule: ConcertSchedule = concertSchedule
        protected set

    @Column(name = "seat_number", nullable = false)
    var seatNumber: Int = seatNumber
        protected set

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var seatStatus: SeatStatus = seatStatus
        protected set

    @Column(name = "seat_price", nullable = false)
    var seatPrice: Int = seatPrice
        protected set
}
