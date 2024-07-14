package com.hhplus.concert.infra.jpa

import com.hhplus.concert.infra.entity.ConcertSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface ConcertScheduleJpaRepository : JpaRepository<ConcertSchedule, Long> {
    fun findAllByConcertId(concertId: Long): List<ConcertSchedule>
}
