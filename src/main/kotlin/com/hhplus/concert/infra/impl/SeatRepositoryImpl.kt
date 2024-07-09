package com.hhplus.concert.infra.impl

import com.hhplus.concert.domain.repository.SeatRepository
import com.hhplus.concert.infra.entity.Seat
import com.hhplus.concert.infra.jpa.SeatJpaRepository
import org.springframework.stereotype.Repository

@Repository
class SeatRepositoryImpl(
    private val seatJpaRepository: SeatJpaRepository,
) : SeatRepository {
    override fun findAllByScheduleId(scheduleId: Long): List<Seat> = seatJpaRepository.findAllByScheduleId(scheduleId)
}
