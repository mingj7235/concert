package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.business.domain.entity.ConcertSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface ConcertScheduleJpaRepository : JpaRepository<ConcertSchedule, Long> {
    fun findAllByConcertId(concertId: Long): List<ConcertSchedule>
}
