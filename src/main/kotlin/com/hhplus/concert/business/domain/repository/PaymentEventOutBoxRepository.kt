package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.business.domain.entity.PaymentEventOutBox

interface PaymentEventOutBoxRepository {
    fun save(paymentEventOutBox: PaymentEventOutBox): PaymentEventOutBox

    fun findByPaymentId(paymentId: Long): PaymentEventOutBox
}
