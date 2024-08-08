package com.hhplus.concert.business.domain.manager.payment

interface PaymentEventPublisher {
    fun publishPaymentEvent(event: PaymentEvent)
}
