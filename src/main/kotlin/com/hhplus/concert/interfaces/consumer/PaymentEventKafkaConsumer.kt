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
    var receivedMessage: String? = null

    /**
     * Kafka 가 이벤트를 잘 Consume 하는 지 테스트 하기 위한 메서드
     */
    @KafkaListener(topics = ["test_topic"], groupId = "test-group")
    fun consume(message: String) {
        println("Received message: $message")
        receivedMessage = message
    }

    @Async
    @KafkaListener(topics = ["payment-event"], groupId = "concert")
    fun handleSendMessageKafkaEvent(paymentId: String) {
        logger.info("KafkaEvent received")
        paymentService.sendPaymentEventMessage(paymentId.toLong())
    }

    private val logger = LoggerFactory.getLogger(this::class.java)
}
