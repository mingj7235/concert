package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.common.type.SeatStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface SeatJpaRepository : JpaRepository<Seat, Long> {
    @Query("select seat from Seat seat where seat.concertSchedule.id = :scheduleId")
    fun findAllByScheduleId(scheduleId: Long): List<Seat>

    @Query("SELECT s FROM Seat s WHERE s.id in :seatIds")
    fun finaAllByIdWithLock(seatIds: List<Long>): List<Seat>

    @Modifying(clearAutomatically = true)
    @Query("update Seat seat set seat.seatStatus = :status where seat.id in :seatIds")
    fun updateAllStatus(
        seatIds: List<Long>,
        status: SeatStatus,
    )
}
