package com.hhplus.concert.application.facade.unittest

import com.hhplus.concert.business.application.service.PaymentService
import com.hhplus.concert.business.domain.entity.Concert
import com.hhplus.concert.business.domain.entity.ConcertSchedule
import com.hhplus.concert.business.domain.entity.Payment
import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.manager.UserManager
import com.hhplus.concert.business.domain.manager.concert.ConcertCacheManager
import com.hhplus.concert.business.domain.manager.concert.ConcertManager
import com.hhplus.concert.business.domain.manager.payment.PaymentManager
import com.hhplus.concert.business.domain.manager.queue.QueueManager
import com.hhplus.concert.business.domain.manager.reservation.ReservationManager
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.PaymentStatus
import com.hhplus.concert.common.type.QueueStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class PaymentServiceTest {
    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var reservationManager: ReservationManager

    @Mock
    private lateinit var paymentManager: PaymentManager

    @Mock
    private lateinit var queueManager: QueueManager

    @Mock
    private lateinit var concertManager: ConcertManager

    @Mock
    private lateinit var concertCacheManager: ConcertCacheManager

    @InjectMocks
    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `정상적으로 결제가 성공한다`() {
        // Given
        val token = "test-token"
        val userId = 1L
        val reservationIds = listOf(1L, 2L)

        val user = mock(User::class.java)
        val reservation1 = mock(Reservation::class.java)
        val reservation2 = mock(Reservation::class.java)
        val seat1 = mock(Seat::class.java)
        val seat2 = mock(Seat::class.java)
        val concert1 = mock(Concert::class.java)
        val concert2 = mock(Concert::class.java)
        val concertSchedule1 = mock(ConcertSchedule::class.java)
        val concertSchedule2 = mock(ConcertSchedule::class.java)
        val payment1 = mock(Payment::class.java)
        val payment2 = mock(Payment::class.java)

        `when`(userManager.findById(userId)).thenReturn(user)
        `when`(reservationManager.findAllById(reservationIds)).thenReturn(listOf(reservation1, reservation2))
        `when`(reservation1.user).thenReturn(user)
        `when`(reservation2.user).thenReturn(user)
        `when`(reservation1.seat).thenReturn(seat1)
        `when`(reservation2.seat).thenReturn(seat2)
        `when`(concertSchedule1.concert).thenReturn(concert1)
        `when`(concertSchedule2.concert).thenReturn(concert2)
        `when`(concert1.id).thenReturn(1L)
        `when`(concert2.id).thenReturn(2L)
        `when`(seat1.concertSchedule).thenReturn(concertSchedule1)
        `when`(seat2.concertSchedule).thenReturn(concertSchedule2)
        `when`(user.id).thenReturn(userId)
        `when`(paymentManager.executeAndSaveHistory(user, listOf(reservation1, reservation2))).thenReturn(listOf(payment1, payment2))
        `when`(queueManager.getQueueStatus(token)).thenReturn(QueueStatus.PROCESSING)

        `when`(payment1.id).thenReturn(1L)
        `when`(payment1.amount).thenReturn(10000)
        `when`(payment1.paymentStatus).thenReturn(PaymentStatus.COMPLETED)
        `when`(payment2.id).thenReturn(2L)
        `when`(payment2.amount).thenReturn(20000)
        `when`(payment2.paymentStatus).thenReturn(PaymentStatus.COMPLETED)

        // When
        val result = paymentService.executePayment(token, userId, reservationIds)

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].paymentId)
        assertEquals(10000, result[0].amount)
        assertEquals(PaymentStatus.COMPLETED, result[0].paymentStatus)
        assertEquals(2L, result[1].paymentId)
        assertEquals(20000, result[1].amount)
        assertEquals(PaymentStatus.COMPLETED, result[1].paymentStatus)

        verify(reservationManager).complete(listOf(reservation1, reservation2))
        verify(queueManager).completeProcessingToken(token)
    }

    @Test
    fun `유효하지 않은 사용자로 결제 시도 시 예외가 발생한다`() {
        // Given
        val token = "test-token"
        val userId = 1L
        val reservationIds = listOf(1L, 2L)

        val user = mock(User::class.java)
        val reservation1 = mock(Reservation::class.java)
        val reservation2 = mock(Reservation::class.java)
        val invalidUser = mock(User::class.java)

        `when`(userManager.findById(userId)).thenReturn(user)
        `when`(reservationManager.findAllById(reservationIds)).thenReturn(listOf(reservation1, reservation2))
        `when`(reservation1.user).thenReturn(user)
        `when`(reservation2.user).thenReturn(invalidUser)
        `when`(user.id).thenReturn(userId)
        `when`(invalidUser.id).thenReturn(2L)

        // When & Then
        assertThrows(BusinessException.BadRequest::class.java) {
            paymentService.executePayment(token, userId, reservationIds)
        }
    }
}
