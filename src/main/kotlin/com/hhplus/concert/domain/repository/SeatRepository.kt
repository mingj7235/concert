package com.hhplus.concert.domain.repository

import com.hhplus.concert.infra.entity.Seat

interface SeatRepository {
    fun findAllByScheduleId(scheduleId: Long): List<Seat>
}
