package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.entity.Payment
import com.hhplus.concert.business.domain.repository.PaymentRepository
import com.hhplus.concert.infrastructure.jpa.PaymentJpaRepository
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    override fun save(payment: Payment): Payment = paymentJpaRepository.save(payment)

    override fun findByReservationId(reservationId: Long): Payment? = paymentJpaRepository.findByReservationId(reservationId)

    override fun findById(paymentId: Long): Payment? = paymentJpaRepository.findById(paymentId).getOrNull()
}
