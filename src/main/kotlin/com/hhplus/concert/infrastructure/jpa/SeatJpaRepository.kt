package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.infrastructure.entity.Seat
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface SeatJpaRepository : JpaRepository<Seat, Long> {
    @Query("select seat from Seat seat where seat.concertSchedule.id = :scheduleId")
    fun findAllByScheduleId(scheduleId: Long): List<Seat>

    @Modifying
    @Query("update Seat seat set seat.seatStatus = :status where seat.id in :seatIds")
    fun updateAllStatus(
        seatIds: List<Long>,
        status: SeatStatus,
    ): List<Long>
}
