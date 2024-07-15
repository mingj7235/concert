package com.hhplus.concert.interfaces.presentation.request

import com.hhplus.concert.business.application.dto.ReservationServiceDto

object ReservationRequest {
    data class Detail(
        val userId: Long,
        val concertId: Long,
        val scheduleId: Long,
        val seatIds: List<Long>,
    ) {
        companion object {
            fun toDto(request: Detail): ReservationServiceDto.Request =
                ReservationServiceDto.Request(
                    userId = request.userId,
                    concertId = request.concertId,
                    scheduleId = request.scheduleId,
                    seatIds = request.seatIds,
                )
        }
    }
}
