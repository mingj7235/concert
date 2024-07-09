package com.hhplus.concert.domain.manager.concert

import com.hhplus.concert.common.exception.error.ConcertException
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.domain.repository.ConcertRepository
import com.hhplus.concert.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.domain.repository.SeatRepository
import com.hhplus.concert.infra.entity.Concert
import com.hhplus.concert.infra.entity.ConcertSchedule
import com.hhplus.concert.infra.entity.Seat
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

        val concertSchedule = concertScheduleRepository.findById(scheduleId)
        if (!validateScheduleReservationTime(
                reservationAvailableAt = concertSchedule.reservationAvailableAt,
                concertAt = concertSchedule.concertAt,
            )
        ) {
            throw ConcertException.UnAvailable()
        }

        return seatRepository.findAllByScheduleId(scheduleId)
    }

    private fun validateConcertStatus(concertId: Long) {
        val concert = concertRepository.findById(concertId)
        if (concert.concertStatus == ConcertStatus.UNAVAILABLE) throw ConcertException.UnAvailable()
    }

    private fun validateScheduleReservationTime(
        reservationAvailableAt: LocalDateTime,
        concertAt: LocalDateTime,
    ): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(reservationAvailableAt) && now.isBefore(concertAt)
    }
}
