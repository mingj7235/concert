package com.hhplus.concert.domain.repository

import com.hhplus.concert.infra.entity.PaymentHistory

interface PaymentHistoryRepository {
    fun save(paymentHistory: PaymentHistory)
}
