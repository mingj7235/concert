package com.hhplus.concert.presentation

import com.hhplus.concert.application.facade.ReservationService
import com.hhplus.concert.common.annotation.TokenRequired
import com.hhplus.concert.common.annotation.ValidatedToken
import com.hhplus.concert.presentation.request.ReservationRequest
import com.hhplus.concert.presentation.response.ReservationResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ReservationController(
    private val reservationService: ReservationService,
) {
    // 좌석을 예약한다.
    @TokenRequired
    @PostMapping("/reservations")
    fun createReservations(
        @ValidatedToken token: String,
        @RequestBody reservationRequest: ReservationRequest.Detail,
    ): List<ReservationResponse.Result> =
        reservationService
            .createReservations(
                token = token,
                reservationRequest = ReservationRequest.Detail.toDto(reservationRequest),
            ).map {
                ReservationResponse.Result.from(it)
            }
}
