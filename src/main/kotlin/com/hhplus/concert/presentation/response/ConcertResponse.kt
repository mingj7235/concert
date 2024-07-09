package com.hhplus.concert.presentation.response

import com.hhplus.concert.application.dto.ConcertServiceDto
import com.hhplus.concert.common.type.SeatStatus
import java.time.LocalDateTime

object ConcertResponse {
    data class Concert(
        val concertId: Long,
        val title: String,
        val description: String,
    ) {
        companion object {
            fun from(concertDto: ConcertServiceDto.Concert): Concert =
                Concert(
                    concertId = concertDto.concertId,
                    title = concertDto.title,
                    description = concertDto.description,
                )
        }
    }

    data class Schedule(
        val concertId: Long,
        val events: List<Event>,
    ) {
        companion object {
            fun from(scheduleDto: ConcertServiceDto.Schedule): Schedule =
                Schedule(
                    concertId = scheduleDto.concertId,
                    events = scheduleDto.events.map { Event.from(it) },
                )
        }
    }

    data class Event(
        val scheduleId: Long,
        val concertAt: LocalDateTime,
        val reservationAt: LocalDateTime,
    ) {
        companion object {
            fun from(eventDto: ConcertServiceDto.Event): Event =
                Event(
                    scheduleId = eventDto.scheduleId,
                    concertAt = eventDto.concertAt,
                    reservationAt = eventDto.reservationAt,
                )
        }
    }

    data class AvailableSeat(
        val concertId: Long,
        val seats: List<Seat>,
    ) {
        companion object {
            fun from(availableSeatDto: ConcertServiceDto.AvailableSeat): AvailableSeat =
                AvailableSeat(
                    concertId = availableSeatDto.concertId,
                    seats =
                        availableSeatDto.seats.map {
                            Seat.from(it)
                        },
                )
        }
    }

    data class Seat(
        val seatId: Long,
        val seatNumber: Int,
        val seatStatus: SeatStatus,
        val seatPrice: Int,
    ) {
        companion object {
            fun from(seatDto: ConcertServiceDto.Seat): Seat =
                Seat(
                    seatId = seatDto.seatId,
                    seatNumber = seatDto.seatNumber,
                    seatStatus = seatDto.seatStatus,
                    seatPrice = seatDto.seatPrice,
                )
        }
    }
}
