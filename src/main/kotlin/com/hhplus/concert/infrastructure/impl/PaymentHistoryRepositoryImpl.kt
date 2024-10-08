package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.entity.PaymentHistory
import com.hhplus.concert.business.domain.repository.PaymentHistoryRepository
import com.hhplus.concert.infrastructure.jpa.PaymentHistoryJpaRepository
import org.springframework.stereotype.Repository

@Repository
class PaymentHistoryRepositoryImpl(
    private val paymentHistoryJpaRepository: PaymentHistoryJpaRepository,
) : PaymentHistoryRepository {
    override fun save(paymentHistory: PaymentHistory) {
        paymentHistoryJpaRepository.save(paymentHistory)
    }

    override fun findAllByPaymentId(paymentId: Long): List<PaymentHistory> = paymentHistoryJpaRepository.findAllByPaymentId(paymentId)
}
