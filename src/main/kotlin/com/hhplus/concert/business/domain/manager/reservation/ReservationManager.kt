package com.hhplus.concert.business.domain.manager.reservation

import com.hhplus.concert.business.application.dto.ReservationServiceDto
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.ReservationRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.exception.error.ConcertException
import com.hhplus.concert.common.exception.error.UserException
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.infrastructure.entity.Reservation
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ReservationManager(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val concertRepository: ConcertRepository,
    private val concertScheduleRepository: ConcertScheduleRepository,
    private val seatRepository: SeatRepository,
) {
    /**
     * 1. Reservation 을 PaymentPending 상태로 생성한다.
     * 2. 좌석 상태를 Unavailable 로 변경한다.
     */
    fun createReservations(reservationRequest: ReservationServiceDto.Request): List<Reservation> {
        val user = userRepository.findById(reservationRequest.userId) ?: throw UserException.UserNotFound()
        val concert = concertRepository.findById(reservationRequest.concertId) ?: throw ConcertException.NotFound()
        val concertSchedule = concertScheduleRepository.findById(reservationRequest.scheduleId)
        val seats = seatRepository.findAllById(reservationRequest.seatIds)

        val reservations =
            seats.map { seat ->
                val reservation =
                    Reservation(
                        user = user,
                        concertTitle = concert.title,
                        concertAt = concertSchedule.concertAt,
                        seat = seat,
                        reservationStatus = ReservationStatus.PAYMENT_PENDING,
                        createdAt = LocalDateTime.now(),
                    )
                reservationRepository.save(reservation)
            }

        seatRepository.updateAllStatus(reservationRequest.seatIds, SeatStatus.UNAVAILABLE)

        return reservations
    }

    /**
     * 1. 만기된 (예약후 5분 이내에 결제가 완료되지 않은) 예약건들을 조회한다.
     * 2. 조회된 예약건들의 상태를 변경한다.
     * 3. 조회된 좌석의 상태를 변경한다.
     */
    fun cancelReservations() {
        val expirationTime = LocalDateTime.now().minusMinutes(RESERVATION_EXPIRATION_MINUTE)
        val unpaidReservations =
            reservationRepository.findExpiredReservations(
                ReservationStatus.PAYMENT_PENDING,
                expirationTime,
            )

        reservationRepository.updateAllStatus(
            reservationIds = unpaidReservations.map { it.id },
            reservationStatus = ReservationStatus.RESERVATION_CANCELLED,
        )

        seatRepository.updateAllStatus(
            seatIds = unpaidReservations.map { it.seat.id },
            status = SeatStatus.AVAILABLE,
        )
    }

    fun findAllById(reservationIds: List<Long>): List<Reservation> = reservationRepository.findAllById(reservationIds)

    fun complete(requestReservations: List<Reservation>) {
        reservationRepository.updateAllStatus(
            reservationIds = requestReservations.map { it.id },
            ReservationStatus.PAYMENT_COMPLETED,
        )
    }

    companion object {
        const val RESERVATION_EXPIRATION_MINUTE = 5L
    }
}