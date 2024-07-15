package com.hhplus.concert.interfaces.presentation.request

object PaymentRequest {
    data class Detail(
        val reservationIds: List<Long>,
    )
}
