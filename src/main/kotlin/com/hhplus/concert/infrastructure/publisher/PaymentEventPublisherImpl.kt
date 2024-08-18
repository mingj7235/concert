package com.hhplus.concert.infrastructure.publisher

import com.hhplus.concert.business.domain.manager.payment.event.PaymentEvent
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEventPublisher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
@Qualifier("application")
class PaymentEventPublisherImpl(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : PaymentEventPublisher {
    override fun publishPaymentEvent(event: PaymentEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
