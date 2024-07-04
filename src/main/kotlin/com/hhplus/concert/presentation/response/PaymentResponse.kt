package com.hhplus.concert.presentation.response

import com.hhplus.concert.common.type.PaymentStatus

object PaymentResponse {
    data class Result(
        val paymentId: Long,
        val amount: Long,
        val paymentStatus: PaymentStatus,
    )
}
