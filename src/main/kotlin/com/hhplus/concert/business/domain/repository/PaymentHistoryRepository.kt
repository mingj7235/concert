package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.business.domain.entity.PaymentHistory

interface PaymentHistoryRepository {
    fun save(paymentHistory: PaymentHistory)

    fun findAllByPaymentId(paymentId: Long): List<PaymentHistory>
}
