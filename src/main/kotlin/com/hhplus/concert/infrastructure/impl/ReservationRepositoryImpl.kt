package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.business.domain.repository.ReservationRepository
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.infrastructure.jpa.ReservationJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Repository
class ReservationRepositoryImpl(
    private val reservationJpaRepository: ReservationJpaRepository,
) : ReservationRepository {
    override fun save(reservation: Reservation): Reservation = reservationJpaRepository.save(reservation)

    override fun findAll(): List<Reservation> = reservationJpaRepository.findAll()

    override fun findExpiredReservations(
        reservationStatus: ReservationStatus,
        expirationTime: LocalDateTime,
    ): List<Reservation> = reservationJpaRepository.findExpiredReservations(reservationStatus, expirationTime)

    override fun updateAllStatus(
        reservationIds: List<Long>,
        reservationStatus: ReservationStatus,
    ) {
        reservationJpaRepository.updateAllStatus(reservationIds, reservationStatus)
    }

    override fun findAllById(reservationIds: List<Long>): List<Reservation> = reservationJpaRepository.findAllByIdFetchSeat(reservationIds)

    override fun findById(reservationId: Long): Reservation? = reservationJpaRepository.findById(reservationId).getOrNull()
}
