package com.hhplus.concert.interfaces.presentation.response

import com.hhplus.concert.business.application.dto.PaymentServiceDto
import com.hhplus.concert.common.type.PaymentStatus

class PaymentResponse {
    data class Result(
        val paymentId: Long,
        val amount: Int,
        val paymentStatus: PaymentStatus,
    ) {
        companion object {
            fun from(resultDto: PaymentServiceDto.Result): Result =
                Result(
                    paymentId = resultDto.paymentId,
                    amount = resultDto.amount,
                    paymentStatus = resultDto.paymentStatus,
                )
        }
    }
}
