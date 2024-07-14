package com.hhplus.concert.infra.impl

import com.hhplus.concert.domain.repository.PaymentRepository
import com.hhplus.concert.infra.entity.Payment
import com.hhplus.concert.infra.jpa.PaymentJpaRepository
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    override fun save(payment: Payment): Payment = paymentJpaRepository.save(payment)
}
