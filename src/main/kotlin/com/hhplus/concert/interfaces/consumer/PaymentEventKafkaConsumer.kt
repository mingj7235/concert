package com.hhplus.concert.interfaces.consumer

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PaymentEventKafkaConsumer {
    var receivedMessage: String? = null

    /**
     * Kafka 가 이벤트를 잘 Consume 하는 지 테스트 하기 위한 메서드
     */
    @KafkaListener(topics = ["test_topic"], groupId = "test-group")
    fun consume(message: String) {
        println("Received message: $message")
        receivedMessage = message
    }
}
