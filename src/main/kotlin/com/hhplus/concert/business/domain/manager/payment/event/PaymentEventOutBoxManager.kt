package com.hhplus.concert.business.domain.manager.payment.event

import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import com.hhplus.concert.business.domain.repository.PaymentEventOutBoxRepository
import com.hhplus.concert.common.type.EventStatus
import org.springframework.stereotype.Component

@Component
class PaymentEventOutBoxManager(
    private val paymentEventOutBoxRepository: PaymentEventOutBoxRepository,
) {
    fun saveEventOutBox(
        domainId: Long,
        eventStatus: EventStatus,
    ): PaymentEventOutBox =
        paymentEventOutBoxRepository.save(
            PaymentEventOutBox(
                paymentId = domainId,
                eventStatus = eventStatus,
            ),
        )

    fun findEventByPaymentId(paymentId: Long): PaymentEventOutBox = paymentEventOutBoxRepository.findByPaymentId(paymentId)

    fun updateEventStatus(
        paymentId: Long,
        eventStatus: EventStatus,
    )  {
        val eventOutbox = paymentEventOutBoxRepository.findByPaymentId(paymentId)
        eventOutbox.updateEventStatus(eventStatus)
    }
}
