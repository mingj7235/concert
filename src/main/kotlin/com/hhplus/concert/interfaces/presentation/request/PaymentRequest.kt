package com.hhplus.concert.interfaces.presentation.request

class PaymentRequest {
    data class Detail(
        val reservationIds: List<Long>,
    )
}
