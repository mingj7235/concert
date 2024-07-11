package com.hhplus.concert.presentation.request

object PaymentRequest {
    data class Detail(
        val reservationIds: List<Long>,
    )
}
