package com.hhplus.concert.interfaces.scheduler

import com.hhplus.concert.business.application.service.ReservationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReservationScheduler(
    private val reservationService: ReservationService,
) {
    private val logger = LoggerFactory.getLogger(ReservationScheduler::class.java)

    @Scheduled(fixedRate = 60000)
    fun cancelExpiredReservations() {
        logger.info("Cancel Expired Reservation Scheduler Executed")
        reservationService.cancelUnpaidReservationsAndReleaseSeats()
    }
}
