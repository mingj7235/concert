package com.hhplus.concert.infrastructure.kafka

import com.hhplus.concert.business.domain.manager.payment.event.PaymentEvent
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEventPublisher
import com.hhplus.concert.business.domain.repository.PaymentEventOutBoxRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@Qualifier("kafka")
class PaymentEventKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val paymentEventOutBoxRepository: PaymentEventOutBoxRepository,
) : PaymentEventPublisher {
    /**
     * Kafka Template 을 통해 Kafka 에게 message 를 전송하는지 확인하는 메서드
     */
    fun send(
        topic: String,
        message: String,
    ) {
        kafkaTemplate.send(topic, message)
    }

    override fun publishPaymentEvent(event: PaymentEvent) {
        TODO("Not yet implemented")
    }
}
