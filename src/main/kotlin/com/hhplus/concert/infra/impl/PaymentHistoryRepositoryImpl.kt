package com.hhplus.concert.infra.impl

import com.hhplus.concert.domain.repository.PaymentHistoryRepository
import com.hhplus.concert.infra.entity.PaymentHistory
import com.hhplus.concert.infra.jpa.PaymentHistoryJpaRepository
import org.springframework.stereotype.Repository

@Repository
class PaymentHistoryRepositoryImpl(
    private val paymentHistoryJpaRepository: PaymentHistoryJpaRepository,
) : PaymentHistoryRepository {
    override fun save(paymentHistory: PaymentHistory) {
        paymentHistoryJpaRepository.save(paymentHistory)
    }
}
