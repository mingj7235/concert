package com.hhplus.concert.business.domain.repository

import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import java.time.LocalDateTime

interface PaymentEventOutBoxRepository {
    fun save(paymentEventOutBox: PaymentEventOutBox): PaymentEventOutBox

    fun findByPaymentId(paymentId: Long): PaymentEventOutBox?

    fun findAllFailedEvent(dateTime: LocalDateTime): List<PaymentEventOutBox>

    fun deleteAllPublishedEvent(dateTime: LocalDateTime)
}
