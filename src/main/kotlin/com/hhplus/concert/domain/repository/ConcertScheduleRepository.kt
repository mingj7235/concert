package com.hhplus.concert.domain.repository

import com.hhplus.concert.infra.entity.ConcertSchedule

interface ConcertScheduleRepository {
    fun findAllByConcertId(concertId: Long): List<ConcertSchedule>

    fun findById(scheduleId: Long): ConcertSchedule
}
