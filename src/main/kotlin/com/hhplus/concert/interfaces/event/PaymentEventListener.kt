package com.hhplus.concert.interfaces.event

import com.hhplus.concert.business.application.service.PaymentService
import com.hhplus.concert.business.domain.manager.payment.PaymentEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventListener(
    private val paymentService: PaymentService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun listenMessageEvent(event: PaymentEvent) {
        kotlin
            .runCatching {
                paymentService.sendPaymentEventMessage(event.paymentId)
            }.onFailure { e ->
                logger.error("Payment Event Listener Error : ${e.message}")
            }
    }

    private val logger = LoggerFactory.getLogger(PaymentEventListener::class.java)
}
