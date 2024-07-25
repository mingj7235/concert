package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.common.type.SeatStatus

interface SeatRepository {
    fun findAllByScheduleId(scheduleId: Long): List<Seat>

    fun findAllById(seatIds: List<Long>): List<Seat>

    fun findAllByIdAndStatusWithPessimisticLock(
        seatIds: List<Long>,
        seatStatus: SeatStatus,
    ): List<Seat>

    fun updateAllStatus(
        seatIds: List<Long>,
        status: SeatStatus,
    )

    fun save(seat: Seat): Seat

    fun findById(seatId: Long): Seat?
}
