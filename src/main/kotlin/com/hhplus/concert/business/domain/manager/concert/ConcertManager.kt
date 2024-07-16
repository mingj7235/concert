package com.hhplus.concert.business.domain.manager.concert

import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.infrastructure.entity.Concert
import com.hhplus.concert.infrastructure.entity.ConcertSchedule
import com.hhplus.concert.infrastructure.entity.Seat
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ConcertManager(
    private val concertRepository: ConcertRepository,
    private val concertScheduleRepository: ConcertScheduleRepository,
    private val seatRepository: SeatRepository,
) {
    /**
     * 예약 가능한 Concert 리스트를 조회한다.
     */
    fun getAvailableConcerts(): List<Concert> =
        concertRepository
            .findAll()
            .filter { it.concertStatus == ConcertStatus.AVAILABLE }

    /**
     * 예약 가능한 Concert 의 스케쥴 리스트를 조회한다.
     * 각 스케쥴이 예약가능한 시간인 스케쥴만 리턴한다.
     */
    fun getAvailableConcertSchedules(concertId: Long): List<ConcertSchedule> {
        validateConcertStatus(concertId)

        return concertScheduleRepository
            .findAllByConcertId(concertId)
            .filter {
                validateScheduleReservationTime(
                    reservationAvailableAt = it.reservationAvailableAt,
                    concertAt = it.concertAt,
                )
            }
    }

    /**
     * concertId 와 scheduleId 로 Seat 리스트를 조회한다.
     * concert 는 예약 가능해야한다.
     * concertSchedule 은 예약 가능해야한다.
     */
    fun getAvailableSeats(
        concertId: Long,
        scheduleId: Long,
    ): List<Seat> {
        validateConcertStatus(concertId)

        val concertSchedule =
            concertScheduleRepository.findById(scheduleId)
                ?: throw BusinessException.NotFound(ErrorCode.Concert.SCHEDULE_NOT_FOUND)
        if (!validateScheduleReservationTime(
                reservationAvailableAt = concertSchedule.reservationAvailableAt,
                concertAt = concertSchedule.concertAt,
            )
        ) {
            throw BusinessException.BadRequest(ErrorCode.Concert.UNAVAILABLE)
        }

        return seatRepository.findAllByScheduleId(scheduleId)
    }

    private fun validateConcertStatus(concertId: Long) {
        val concert = concertRepository.findById(concertId) ?: throw BusinessException.NotFound(ErrorCode.Concert.NOT_FOUND)
        if (concert.concertStatus == ConcertStatus.UNAVAILABLE) throw BusinessException.BadRequest(ErrorCode.Concert.UNAVAILABLE)
    }

    private fun validateScheduleReservationTime(
        reservationAvailableAt: LocalDateTime,
        concertAt: LocalDateTime,
    ): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(reservationAvailableAt) && now.isBefore(concertAt)
    }
}
