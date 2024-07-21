package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.common.type.ReservationStatus
import java.time.LocalDateTime

interface ReservationRepository {
    fun save(reservation: Reservation): Reservation

    fun findAll(): List<Reservation>

    fun findExpiredReservations(
        reservationStatus: ReservationStatus,
        expirationTime: LocalDateTime,
    ): List<Reservation>

    fun updateAllStatus(
        reservationIds: List<Long>,
        reservationStatus: ReservationStatus,
    )

    fun findAllById(reservationIds: List<Long>): List<Reservation>

    fun findById(reservationId: Long): Reservation?
}
