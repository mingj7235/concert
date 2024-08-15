package com.hhplus.concert.infrastructure.kafka

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class PaymentEventKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    /**
     * Kafka Template 을 통해 Kafka 에게 message 를 전송하는지 확인하는 메서드
     */
    fun send(
        topic: String,
        message: String,
    ) {
        kafkaTemplate.send(topic, message)
    }
}
