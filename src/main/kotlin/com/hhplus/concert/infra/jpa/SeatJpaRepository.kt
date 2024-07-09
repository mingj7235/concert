package com.hhplus.concert.infra.jpa

import com.hhplus.concert.infra.entity.Seat
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SeatJpaRepository : JpaRepository<Seat, Long> {
    @Query("select seat from Seat seat where seat.concertSchedule.id = :scheduleId")
    fun findAllByScheduleId(scheduleId: Long): List<Seat>
}
