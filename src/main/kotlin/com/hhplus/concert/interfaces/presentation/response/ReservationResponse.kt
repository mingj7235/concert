package com.hhplus.concert.interfaces.presentation.response

import com.hhplus.concert.business.application.dto.ReservationServiceDto
import com.hhplus.concert.common.type.ReservationStatus
import java.time.LocalDateTime

class ReservationResponse {
    data class Result(
        val reservationId: Long,
        val concertId: Long,
        val concertName: String,
        val concertAt: LocalDateTime,
        val seat: Seat,
        val reservationStatus: ReservationStatus,
    ) {
        companion object {
            fun from(resultDto: ReservationServiceDto.Result): Result =
                Result(
                    reservationId = resultDto.reservationId,
                    concertId = resultDto.concertId,
                    concertName = resultDto.concertName,
                    concertAt = resultDto.concertAt,
                    seat = Seat.from(resultDto.seat),
                    reservationStatus = resultDto.reservationStatus,
                )
        }
    }

    data class Seat(
        val seatNumber: Int,
        val price: Int,
    ) {
        companion object {
            fun from(seatDto: ReservationServiceDto.Seat): Seat =
                Seat(
                    seatNumber = seatDto.seatNumber,
                    price = seatDto.price,
                )
        }
    }
}
