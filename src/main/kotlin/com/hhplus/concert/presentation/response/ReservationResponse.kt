package com.hhplus.concert.presentation.response

import com.hhplus.concert.common.type.ReservationStatus
import java.time.LocalDateTime

object ReservationResponse {
    data class Result(
        val reservationId: Long,
        val concertId: Long,
        val concertName: String,
        val concertAt: LocalDateTime,
        val seats: List<Seat>,
        val totalPrice: Int,
        val reservationStatus: ReservationStatus,
    )

    data class Seat(
        val seatNumber: Int,
        val price: Int,
    )
}
