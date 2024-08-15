package com.hhplus.concert.business.application.service

import com.hhplus.concert.business.application.dto.PaymentEventOutBoxServiceDto
import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEvent
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEventOutBoxManager
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEventPublisher
import com.hhplus.concert.common.type.EventStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PaymentEventOutBoxService(
    private val paymentEventOutBoxManager: PaymentEventOutBoxManager,
    @Qualifier("kafka") private val paymentEventPublisher: PaymentEventPublisher,
) {
    /**
     * outbox 를 저장한다.
     */
    fun saveEventOutBox(
        domainId: Long,
        eventStatus: EventStatus,
    ): PaymentEventOutBox =
        paymentEventOutBoxManager.saveEventOutBox(
            domainId = domainId,
            eventStatus = eventStatus,
        )

    /**
     * paymentId 로 eventOutBox 를 찾아온다.
     */
    fun findEventByPaymentId(paymentId: Long): PaymentEventOutBoxServiceDto.EventOutBox {
        val eventOutBox = paymentEventOutBoxManager.findEventByPaymentId(paymentId)
        return PaymentEventOutBoxServiceDto.EventOutBox(
            id = eventOutBox.id,
            paymentId = eventOutBox.paymentId,
            eventStatus = eventOutBox.eventStatus,
        )
    }

    fun updateEventStatus(
        paymentId: Long,
        eventStatus: EventStatus,
    ) {
        paymentEventOutBoxManager.updateEventStatus(
            paymentId = paymentId,
            eventStatus = eventStatus,
        )
    }

    /**
     * kafka 이벤트를 발행한다.
     */
    fun publishPaymentEvent(event: PaymentEvent) {
        paymentEventPublisher.publishPaymentEvent(event)
    }
}
