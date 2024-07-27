package com.hhplus.concert.business.domain.manager.reservation

import com.hhplus.concert.business.application.dto.ReservationServiceDto
import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.common.annotation.DistributedSimpleLock
import org.springframework.stereotype.Component

@Component
class ReservationLockManager(
    private val reservationManager: ReservationManager,
) {
    @DistributedSimpleLock(
        key =
            "'user:' + #reservationRequest.userId + " +
                "'concert:' + #reservationRequest.concertId + " +
                "':schedule:' + #reservationRequest.scheduleId",
        waitTime = 5,
        leaseTime = 10,
    )
    fun createReservations(reservationRequest: ReservationServiceDto.Request): List<Reservation> =
        reservationManager.createReservations(reservationRequest)
}
