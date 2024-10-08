package com.hhplus.concert.infrastructure.impl

import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import com.hhplus.concert.business.domain.repository.PaymentEventOutBoxRepository
import com.hhplus.concert.infrastructure.jpa.EventOutBoxJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PaymentEventOutBoxRepositoryImpl(
    private val eventOutBoxJpaRepository: EventOutBoxJpaRepository,
) : PaymentEventOutBoxRepository {
    override fun save(paymentEventOutBox: PaymentEventOutBox): PaymentEventOutBox = eventOutBoxJpaRepository.save(paymentEventOutBox)

    override fun findByPaymentId(paymentId: Long): PaymentEventOutBox? = eventOutBoxJpaRepository.findByPaymentId(paymentId)

    override fun findAllFailedEvent(dateTime: LocalDateTime): List<PaymentEventOutBox> =
        eventOutBoxJpaRepository.findAllFailedEvent(dateTime)

    override fun deleteAllPublishedEvent(dateTime: LocalDateTime) {
        eventOutBoxJpaRepository.deleteAllPublishedEvent(dateTime)
    }
}
