package com.hhplus.concert.interfaces.scheduler

import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import com.hhplus.concert.business.domain.repository.PaymentEventOutBoxRepository
import com.hhplus.concert.common.type.EventStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentEventSchedulerTest {
    @Autowired
    private lateinit var paymentEventScheduler: PaymentEventScheduler

    @Autowired
    private lateinit var paymentEventOutBoxRepository: PaymentEventOutBoxRepository

    @Test
    fun `5분_이상_지난_INIT_상태의_이벤트를_재시도한다`() {
        // Given
        val oldEvent = PaymentEventOutBox(paymentId = 1, eventStatus = EventStatus.INIT, publishedAt = LocalDateTime.now().minusMinutes(6))
        val recentEvent =
            PaymentEventOutBox(paymentId = 2, eventStatus = EventStatus.INIT, publishedAt = LocalDateTime.now().minusMinutes(4))
        paymentEventOutBoxRepository.save(oldEvent)
        paymentEventOutBoxRepository.save(recentEvent)

        // When
        paymentEventScheduler.retryFailedPaymentEvent()

        // Then
        val updatedOldEvent = paymentEventOutBoxRepository.findByPaymentId(oldEvent.paymentId)
        val updatedRecentEvent = paymentEventOutBoxRepository.findByPaymentId(recentEvent.paymentId)

        assertEquals("PUBLISHED", updatedOldEvent!!.eventStatus)
        assertEquals("INIT", updatedRecentEvent!!.eventStatus)
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
        paymentEventScheduler.deletePublishedPaymentEvent()

        // Then
        assertThat(paymentEventOutBoxRepository.findByPaymentId(oldPublishedEvent.paymentId)).isNull()
        assertThat(paymentEventOutBoxRepository.findByPaymentId(recentPublishedEvent.paymentId)!!.paymentId).isEqualTo(2L)
    }
}
