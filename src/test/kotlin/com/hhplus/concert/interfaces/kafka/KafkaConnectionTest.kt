package com.hhplus.concert.interfaces.kafka

import com.hhplus.concert.infrastructure.kafka.PaymentEventKafkaProducer
import com.hhplus.concert.interfaces.consumer.PaymentEventKafkaConsumer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import java.util.concurrent.TimeUnit

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:29092"], ports = [29092])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class KafkaConnectionTest {
    @Autowired
    private lateinit var paymentEventKafkaProducer: PaymentEventKafkaProducer

    @Autowired
    private lateinit var paymentEventKafkaConsumer: PaymentEventKafkaConsumer

    @Test
    fun `카프카 연결 테스트`() {
        val topic = "test_topic"
        val message = "testMessage"

        paymentEventKafkaProducer.send(topic, message)

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            assertThat(paymentEventKafkaConsumer.receivedMessage).isEqualTo(message)
        }
    }
}
