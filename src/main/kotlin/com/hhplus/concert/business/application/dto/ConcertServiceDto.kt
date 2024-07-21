package com.hhplus.concert.business.application.dto

import com.hhplus.concert.common.type.SeatStatus
import java.time.LocalDateTime

class ConcertServiceDto {
    data class Concert(
        val concertId: Long,
        val title: String,
        val description: String,
    )

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
        val seats: List<Seat>,
    )

    data class Seat(
        val seatId: Long,
        val seatNumber: Int,
        val seatStatus: SeatStatus,
        val seatPrice: Int,
    )
}
