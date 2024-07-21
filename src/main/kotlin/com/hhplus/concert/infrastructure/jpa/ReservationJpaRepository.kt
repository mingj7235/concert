package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.common.type.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ReservationJpaRepository : JpaRepository<Reservation, Long> {
    @Query("select r from Reservation r where r.reservationStatus = :reservationStatus and r.createdAt < :expirationTime")
    fun findExpiredReservations(
        reservationStatus: ReservationStatus,
        expirationTime: LocalDateTime,
    ): List<Reservation>

    @Modifying(clearAutomatically = true)
    @Query("update Reservation r set r.reservationStatus = :reservationStatus where r.id in :reservationIds")
    fun updateAllStatus(
        reservationIds: List<Long>,
        reservationStatus: ReservationStatus,
    )

    @Query("SELECT r FROM Reservation r JOIN FETCH r.seat WHERE r.id IN :reservationIds")
    fun findAllByIdFetchSeat(reservationIds: List<Long>): List<Reservation>
}
