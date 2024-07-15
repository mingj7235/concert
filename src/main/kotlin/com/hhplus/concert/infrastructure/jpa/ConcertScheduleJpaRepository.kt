package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.infrastructure.entity.ConcertSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface ConcertScheduleJpaRepository : JpaRepository<ConcertSchedule, Long> {
    fun findAllByConcertId(concertId: Long): List<ConcertSchedule>
}
