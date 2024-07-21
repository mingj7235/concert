package com.hhplus.concert.business.application.dto

import com.hhplus.concert.common.type.PaymentStatus

class PaymentServiceDto {
    data class Result(
        val paymentId: Long,
        val amount: Int,
        val paymentStatus: PaymentStatus,
    )
}
