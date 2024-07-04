package com.hhplus.concert.presentation.response

import com.hhplus.concert.common.type.SeatStatus
import java.time.LocalDateTime

object ConcertResponse {
    data class Schedule(
        val concertId: Long,
        val events: List<Event>,
    )

    data class Event(
        val scheduleId: Long,
        val concertAt: LocalDateTime,
        val reservationAt: LocalDateTime,
    )

    data class AvailableSeat(
        val concertId: Long,
        val concertAt: LocalDateTime,
        val seats: List<Seat>,
    )

    data class Seat(
        val seatId: Long,
        val seatNumber: Int,
        val seatStatus: SeatStatus,
        val seatPrice: Int,
    )
}
