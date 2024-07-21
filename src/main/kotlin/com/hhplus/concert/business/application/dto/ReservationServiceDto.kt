package com.hhplus.concert.business.application.dto

import com.hhplus.concert.common.type.ReservationStatus
import java.time.LocalDateTime

class ReservationServiceDto {
    data class Request(
        val userId: Long,
        val concertId: Long,
        val scheduleId: Long,
        val seatIds: List<Long>,
    )

    data class Result(
        val reservationId: Long,
        val concertId: Long,
        val concertName: String,
        val concertAt: LocalDateTime,
        val seat: Seat,
        val reservationStatus: ReservationStatus,
    )

    data class Seat(
        val seatNumber: Int,
        val price: Int,
    )
}
