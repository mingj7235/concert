package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.infrastructure.entity.ConcertSchedule

interface ConcertScheduleRepository {
    fun findAllByConcertId(concertId: Long): List<ConcertSchedule>

    fun findById(scheduleId: Long): ConcertSchedule?

    fun save(concertSchedule: ConcertSchedule): ConcertSchedule
}
