package com.hhplus.concert.scheduler

import com.hhplus.concert.application.facade.ReservationService
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
