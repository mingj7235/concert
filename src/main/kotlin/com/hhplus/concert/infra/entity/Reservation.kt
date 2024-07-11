package com.hhplus.concert.infra.entity

import com.hhplus.concert.common.type.ReservationStatus
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
import jakarta.persistence.OneToOne
import java.time.LocalDateTime

@Entity
class Reservation(
    user: User,
    seat: Seat,
    concertTitle: String,
    concertAt: LocalDateTime,
    reservationStatus: ReservationStatus,
    createdAt: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var user: User = user
        protected set

    @Column(name = "concert_title", nullable = false)
    var concertTitle: String = concertTitle
        protected set

    @Column(name = "concert_at", nullable = false)
    var concertAt: LocalDateTime = concertAt
        protected set

    @OneToOne
    @JoinColumn(name = "seat_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var seat: Seat = seat
        protected set

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var reservationStatus: ReservationStatus = reservationStatus
        protected set

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = createdAt
        protected set

    fun updateStatus(status: ReservationStatus) {
        this.reservationStatus = status
    }
}
