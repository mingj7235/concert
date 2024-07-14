package com.hhplus.concert.infra.impl

import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.domain.repository.SeatRepository
import com.hhplus.concert.infra.entity.Seat
import com.hhplus.concert.infra.jpa.SeatJpaRepository
import org.springframework.stereotype.Repository

@Repository
class SeatRepositoryImpl(
    private val seatJpaRepository: SeatJpaRepository,
) : SeatRepository {
    override fun findAllByScheduleId(scheduleId: Long): List<Seat> = seatJpaRepository.findAllByScheduleId(scheduleId)

    override fun findAllById(seatIds: List<Long>): List<Seat> = seatJpaRepository.findAllById(seatIds)

    override fun updateAllStatus(
        seatIds: List<Long>,
        status: SeatStatus,
    ): List<Long> = seatJpaRepository.updateAllStatus(seatIds, status)

    override fun save(seat: Seat) {
        seatJpaRepository.save(seat)
    }
}
