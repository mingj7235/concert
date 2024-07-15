package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.repository.PaymentRepository
import com.hhplus.concert.infrastructure.entity.Payment
import com.hhplus.concert.infrastructure.jpa.PaymentJpaRepository
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    override fun save(payment: Payment): Payment = paymentJpaRepository.save(payment)
}
