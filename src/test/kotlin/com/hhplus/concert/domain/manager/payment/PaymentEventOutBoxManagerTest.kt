package com.hhplus.concert.domain.manager.payment

import com.hhplus.concert.business.domain.entity.PaymentEventOutBox
import com.hhplus.concert.business.domain.manager.payment.event.PaymentEventOutBoxManager
import com.hhplus.concert.business.domain.repository.PaymentEventOutBoxRepository
import com.hhplus.concert.common.type.EventStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PaymentEventOutBoxManagerTest {
    @Mock
    private lateinit var paymentEventOutBoxRepository: PaymentEventOutBoxRepository

    @InjectMocks
    private lateinit var paymentEventOutBoxManager: PaymentEventOutBoxManager

    @BeforeEach
    fun setUp() {
        paymentEventOutBoxManager = PaymentEventOutBoxManager(paymentEventOutBoxRepository)
    }

    @Test
    fun `이벤트 아웃박스 저장 테스트`() {
        // Given
        val paymentId = 1L
        val eventStatus = EventStatus.INIT
        val expectedOutBox = PaymentEventOutBox(paymentId, eventStatus)

        `when`(
            paymentEventOutBoxRepository.save(
                PaymentEventOutBox(paymentId, eventStatus),
            ),
        ).thenReturn(expectedOutBox)

        // When
        val result = paymentEventOutBoxManager.saveEventOutBox(paymentId, eventStatus)

        // Then
        assert(result.paymentId == paymentId)
    }

    @Test
    fun `결제 ID로 이벤트 찾기 테스트`() {
        // Given
        val paymentId = 1L
        val expectedOutBox = PaymentEventOutBox(paymentId, EventStatus.INIT)

        `when`(paymentEventOutBoxRepository.findByPaymentId(paymentId)).thenReturn(expectedOutBox)

        // When
        val result = paymentEventOutBoxManager.findEventByPaymentId(paymentId)

        // Then
        assert(result == expectedOutBox)
    }

    @Test
    fun `이벤트 상태 업데이트 테스트`() {
        // Given
        val paymentId = 1L
        val newEventStatus = EventStatus.PUBLISHED
        val existingOutBox = PaymentEventOutBox(paymentId, EventStatus.INIT)

        `when`(paymentEventOutBoxRepository.findByPaymentId(paymentId)).thenReturn(existingOutBox)

        // When
        paymentEventOutBoxManager.updateEventStatus(paymentId, newEventStatus)

        // Then
        verify(paymentEventOutBoxRepository).findByPaymentId(paymentId)
        assert(existingOutBox.eventStatus == newEventStatus)
    }
}
