package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.common.exception.error.ConcertException
import com.hhplus.concert.infrastructure.entity.ConcertSchedule
import com.hhplus.concert.infrastructure.jpa.ConcertScheduleJpaRepository
import org.springframework.stereotype.Repository

@Repository
class ConcertScheduleRepositoryImpl(
    private val concertScheduleJpaRepository: ConcertScheduleJpaRepository,
) : ConcertScheduleRepository {
    override fun findAllByConcertId(concertId: Long): List<ConcertSchedule> = concertScheduleJpaRepository.findAllByConcertId(concertId)

    override fun findById(scheduleId: Long): ConcertSchedule =
        concertScheduleJpaRepository.findById(scheduleId).orElseThrow {
            ConcertException.NotFoundSchedule()
        }
}
