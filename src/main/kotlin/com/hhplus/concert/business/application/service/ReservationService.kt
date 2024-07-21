package com.hhplus.concert.business.application.service

import com.hhplus.concert.business.application.dto.ReservationServiceDto
import com.hhplus.concert.business.domain.manager.ConcertManager
import com.hhplus.concert.business.domain.manager.QueueManager
import com.hhplus.concert.business.domain.manager.ReservationManager
import com.hhplus.concert.business.domain.manager.UserManager
import com.hhplus.concert.common.error.code.ErrorCode
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.QueueStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationService(
    private val userManager: UserManager,
    private val queueManager: QueueManager,
    private val concertManager: ConcertManager,
    private val reservationManager: ReservationManager,
) {
    /**
     * 1. token 을 통해 queue 를 검증한다. (Processing 상태인지 확인한다.)
     * 2. queue 에서 user 를 추출한다.
     * 3. user, concert, schedule, seat 들의 존재 여부를 검증한다.
     * 3. reservation 을 생성한다.
     * 4. 예약이 완료되면 좌석의 상태를 UNAVAILABLE 로 변경한다.
     */
    @Transactional
    fun createReservations(
        token: String,
        reservationRequest: ReservationServiceDto.Request,
    ): List<ReservationServiceDto.Result> {
        validateQueueStatus(token)

        userManager.findById(reservationRequest.userId)

        validateReservationRequest(
            requestConcertId = reservationRequest.concertId,
            requestScheduleId = reservationRequest.scheduleId,
            requestSeatIds = reservationRequest.seatIds,
        )

        return reservationManager
            .createReservations(reservationRequest)
            .map {
                ReservationServiceDto.Result(
                    reservationId = it.id,
                    concertId = reservationRequest.concertId,
                    concertName = it.concertTitle,
                    concertAt = it.concertAt,
                    seat =
                        ReservationServiceDto.Seat(
                            seatNumber = it.seat.seatNumber,
                            price = it.seat.seatPrice,
                        ),
                    reservationStatus = it.reservationStatus,
                )
            }
    }

    /**
     * 스케쥴러를 통해 1분 간격으로 5분이 지나도 결제가 완료되지 않은 예약건의 상태를 변경한다.
     * - Reservation 은 RESERVATION_CANCELLED 으로 변경한다.
     * - Seat 은 AVAILABLE 변경한다.
     */
    @Transactional
    fun cancelUnpaidReservationsAndReleaseSeats() {
        reservationManager.cancelReservations()
    }

    private fun validateQueueStatus(token: String) {
        val queue = queueManager.findByToken(token)
        if (queue.queueStatus != QueueStatus.PROCESSING) throw BusinessException.BadRequest(ErrorCode.Queue.NOT_ALLOWED)
    }

    private fun validateReservationRequest(
        requestConcertId: Long,
        requestScheduleId: Long,
        requestSeatIds: List<Long>,
    ) {
        val filteredAvailableSeatsCount =
            concertManager
                .getAvailableSeats(
                    concertId = requestConcertId,
                    scheduleId = requestScheduleId,
                ).filter { it.id in requestSeatIds }
                .size

        if (requestSeatIds.size != filteredAvailableSeatsCount) throw BusinessException.BadRequest(ErrorCode.Concert.UNAVAILABLE)
    }
}
