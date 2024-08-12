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
        logger.info("PaymentEventListener")
        paymentService.sendPaymentEventMessage(event.paymentId)
    }

    private val logger = LoggerFactory.getLogger(this::class.java)
}
