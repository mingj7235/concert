package com.hhplus.concert.application.dto

import com.hhplus.concert.common.type.PaymentStatus

object PaymentServiceDto {
    data class Result(
        val paymentId: Long,
        val amount: Int,
        val paymentStatus: PaymentStatus,
    )
}
