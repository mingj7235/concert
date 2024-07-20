package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.infrastructure.entity.Seat
import com.hhplus.concert.infrastructure.jpa.SeatJpaRepository
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class SeatRepositoryImpl(
    private val seatJpaRepository: SeatJpaRepository,
) : SeatRepository {
    override fun findAllByScheduleId(scheduleId: Long): List<Seat> = seatJpaRepository.findAllByScheduleId(scheduleId)

    override fun findAllById(seatIds: List<Long>): List<Seat> = seatJpaRepository.findAllById(seatIds)

    override fun updateAllStatus(
        seatIds: List<Long>,
        status: SeatStatus,
    ) {
        seatJpaRepository.updateAllStatus(seatIds, status)
    }

    override fun save(seat: Seat): Seat = seatJpaRepository.save(seat)

    override fun findById(seatId: Long): Seat? = seatJpaRepository.findById(seatId).getOrNull()
}
