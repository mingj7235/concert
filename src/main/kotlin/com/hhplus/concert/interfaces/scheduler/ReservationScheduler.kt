package com.hhplus.concert.interfaces.scheduler

import com.hhplus.concert.business.application.service.ReservationService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReservationScheduler(
    private val reservationService: ReservationService,
) {
    @Scheduled(fixedRate = 60000)
    fun cancelExpiredReservations() {
        reservationService.cancelUnpaidReservationsAndReleaseSeats()
    }
}
