package com.hhplus.concert.domain.repository

import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.infra.entity.Reservation
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
    ): List<Reservation>

    fun findAllById(reservationIds: List<Long>): List<Reservation>
}
