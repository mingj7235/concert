package com.hhplus.concert.infrastructure.jpa

import com.hhplus.concert.business.domain.entity.PaymentHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PaymentHistoryJpaRepository : JpaRepository<PaymentHistory, Long> {
    @Query("select paymentHistory from PaymentHistory paymentHistory where paymentHistory.payment.id = :paymentId")
    fun findAllByPaymentId(paymentId: Long): List<PaymentHistory>
}
