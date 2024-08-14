package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import org.springframework.data.jpa.repository.JpaRepository

interface EventOutBoxJpaRepository : JpaRepository<PaymentEventOutBox, Long> {
    fun findByPaymentId(paymentId: Long): PaymentEventOutBox
}
