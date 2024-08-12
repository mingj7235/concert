package com.hhplus.concert.infrastructure.publisher

import com.hhplus.concert.business.domain.manager.payment.PaymentEvent
import com.hhplus.concert.business.domain.manager.payment.PaymentEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class PaymentEventPublisherImpl(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : PaymentEventPublisher {
    override fun publishPaymentEvent(event: PaymentEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
