package com.hhplus.concert.presentation.request

object ReservationRequest {
    data class Detail(
        val userId: Long,
        val concertId: Long,
        val scheduleId: Long,
        val seatIds: List<Long>,
    )
}
