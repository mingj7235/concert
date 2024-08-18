package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.service.PaymentEventOutBoxService
import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import com.hhplus.concert.business.domain.repository.PaymentEventOutBoxRepository
import com.hhplus.concert.common.type.EventStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentEventOutBoxServiceIntegrationTest {
    @Autowired
    private lateinit var paymentEventOutBoxService: PaymentEventOutBoxService

    @Autowired
    private lateinit var paymentEventOutBoxRepository: PaymentEventOutBoxRepository

    @MockBean
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Test
    fun `5분_이상_지난_INIT_상태의_이벤트를_재시도하고_Kafka에_발행한다`() {
        // Given
        val oldEvent = PaymentEventOutBox(paymentId = 1, eventStatus = EventStatus.INIT, publishedAt = LocalDateTime.now().minusMinutes(6))
        val recentEvent =
            PaymentEventOutBox(paymentId = 2, eventStatus = EventStatus.INIT, publishedAt = LocalDateTime.now().minusMinutes(4))
        paymentEventOutBoxRepository.save(oldEvent)
        paymentEventOutBoxRepository.save(recentEvent)

        val mockSendResultFuture = mock(CompletableFuture::class.java) as CompletableFuture<SendResult<String, String>>
        `when`(kafkaTemplate.send(anyString(), anyString())).thenReturn(mockSendResultFuture)

        `when`(mockSendResultFuture.whenComplete(any())).thenAnswer {
            val callback = it.arguments[0] as (SendResult<String, String>?, Throwable?) -> Unit
            callback.invoke(mock(SendResult::class.java) as SendResult<String, String>, null)
            mockSendResultFuture
        }

        // When
        paymentEventOutBoxService.retryFailedPaymentEvent()

        // Then
        val updatedOldEvent = paymentEventOutBoxRepository.findByPaymentId(oldEvent.paymentId)
        val updatedRecentEvent = paymentEventOutBoxRepository.findByPaymentId(recentEvent.paymentId)

        assertEquals("PUBLISHED", updatedOldEvent!!.eventStatus)
        assertEquals("INIT", updatedRecentEvent!!.eventStatus)
        verify(kafkaTemplate, times(1)).send("payment-event", "payment1")
        verify(kafkaTemplate, never()).send("payment-event", "payment2")
    }

    @Test
    fun `7일_이상_지난_PUBLISHED_상태의_이벤트를_삭제한다`() {
        // Given
        val oldPublishedEvent =
            PaymentEventOutBox(paymentId = 1, eventStatus = EventStatus.INIT, publishedAt = LocalDateTime.now().minusDays(8))
        val recentPublishedEvent =
            PaymentEventOutBox(paymentId = 2, eventStatus = EventStatus.INIT, publishedAt = LocalDateTime.now().minusDays(6))
        paymentEventOutBoxRepository.save(oldPublishedEvent)
        paymentEventOutBoxRepository.save(recentPublishedEvent)

        // When
        paymentEventOutBoxService.deletePublishedPaymentEvent()

        // Then
        assertThat(paymentEventOutBoxRepository.findByPaymentId(oldPublishedEvent.paymentId)).isNull()
        assertThat(paymentEventOutBoxRepository.findByPaymentId(recentPublishedEvent.paymentId)!!.paymentId).isEqualTo(2L)
    }
}
