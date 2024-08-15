package com.hhplus.concert.interfaces.consumer

import com.hhplus.concert.business.application.service.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class PaymentEventKafkaConsumer(
    private val paymentService: PaymentService,
) {
    @Async
    @KafkaListener(topics = ["payment-event"], groupId = "payment-group")
    fun handleSendMessageKafkaEvent(paymentId: String) {
        logger.info("KafkaEvent received")
        paymentService.sendPaymentEventMessage(paymentId.toLong())
    }

    private val logger = LoggerFactory.getLogger(this::class.java)
}
