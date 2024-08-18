package com.hhplus.concert.business.domain.manager.payment.event

interface PaymentEventPublisher {
    fun publishPaymentEvent(event: PaymentEvent)
}
