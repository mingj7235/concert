package com.hhplus.concert.domain.entity

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

@Entity
class Reservation(
    user: User,
    seat: Seat,
    reservationStatus: ReservationStatus,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var user: User = user
        protected set

    @ManyToOne
    @JoinColumn(name = "seat_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var seat: Seat = seat
        protected set

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var reservationStatus: ReservationStatus = reservationStatus
        protected set
}
