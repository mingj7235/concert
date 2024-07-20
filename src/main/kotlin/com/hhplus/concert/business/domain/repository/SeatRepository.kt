package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.infrastructure.entity.Seat

interface SeatRepository {
    fun findAllByScheduleId(scheduleId: Long): List<Seat>

    fun findAllById(seatIds: List<Long>): List<Seat>

    fun updateAllStatus(
        seatIds: List<Long>,
        status: SeatStatus,
    )

    fun save(seat: Seat): Seat

    fun findById(seatId: Long): Seat?
}
