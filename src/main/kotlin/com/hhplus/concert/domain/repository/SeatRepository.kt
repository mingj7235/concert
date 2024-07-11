package com.hhplus.concert.domain.repository

import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.infra.entity.Seat

interface SeatRepository {
    fun findAllByScheduleId(scheduleId: Long): List<Seat>

    fun findAllById(seatIds: List<Long>): List<Seat>

    fun updateAllStatus(
        seatIds: List<Long>,
        status: SeatStatus,
    ): List<Long>

    fun save(seat: Seat)
}
