package com.hhplus.concert.infrastructure.kafka

import com.hhplus.concert.business.domain.manager.payment.event.PaymentEvent
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEventOutBoxManager
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEventPublisher
import com.hhplus.concert.common.type.EventStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@Qualifier("kafka")
class PaymentEventKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val paymentEventOutBoxManager: PaymentEventOutBoxManager,
) : PaymentEventPublisher {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Kafka Template 을 통해 Kafka 에게 message 를 전송하는지 확인하는 메서드
     */
    fun send(
        topic: String,
        message: String,
    ) {
        kafkaTemplate.send(topic, message)
    }

    /**
     * 카프카 이벤트를 발행한다.
     * - 카프카 발행이 성공한다면 (WhenComplete), OutBox의 상태를 PUBLISHED 로 변경한다.
     */
    override fun publishPaymentEvent(event: PaymentEvent) {
        kafkaTemplate
            .send("payment-event", event.paymentId.toString())
            .whenComplete { _, error ->
                if (error != null) {
                    logger.info("Kafka payment event published")
                    paymentEventOutBoxManager.updateEventStatus(event.paymentId, EventStatus.PUBLISHED)
                }
            }
    }
}
