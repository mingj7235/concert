package com.hhplus.concert.domain.manager.payment

import com.hhplus.concert.business.domain.entity.Payment
import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.manager.payment.PaymentManager
import com.hhplus.concert.business.domain.repository.PaymentHistoryRepository
import com.hhplus.concert.business.domain.repository.PaymentRepository
import com.hhplus.concert.common.type.PaymentStatus
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.domain.manager.reservation.ReservationManagerTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.LocalDateTime

/**
 * PaymentManager 단위 테스트
 */
class PaymentManagerTest {
    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var paymentHistoryRepository: PaymentHistoryRepository

    @InjectMocks
    private lateinit var paymentManager: PaymentManager

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `정상적인 결제 실행 테스트`() {
        // Given
        val user = User("user")
        val reservation1 = RESERVATION1
        val reservation2 = RESERVATION2

        `when`(paymentRepository.save(PAYMENT1)).thenReturn(PAYMENT1)

        // When
        val result = paymentManager.executeAndSaveHistory(user, listOf(reservation1, reservation2))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `결제 실패 시 FAILED 상태로 저장 테스트`() {
        // Given
        val user = mock(User::class.java)
        val reservation1 = mock(Reservation::class.java)
        val seat = mock(Seat::class.java)

        `when`(reservation1.seat).thenReturn(seat)
        `when`(seat.seatPrice).thenReturn(10000)

        `when`(paymentRepository.save(PAYMENT1))
            .thenThrow(RuntimeException("Payment failed"))
            .thenReturn(
                Payment(
                    user = user,
                    reservation = reservation1,
                    amount = 10000,
                    executedAt = LocalDateTime.now(),
                    paymentStatus = PaymentStatus.FAILED,
                ),
            )

        // When
        val result = paymentManager.executeAndSaveHistory(user, listOf(reservation1))

        // Then
        assertEquals(1, result.size)
    }

    @Test
    fun `일부 결제 실패 시 나머지 결제 진행 테스트`() {
        // Given
        val user = mock(User::class.java)
        val reservation1 = mock(Reservation::class.java)
        val reservation2 = mock(Reservation::class.java)
        val seat1 = mock(Seat::class.java)
        val seat2 = mock(Seat::class.java)

        `when`(reservation1.seat).thenReturn(seat1)
        `when`(reservation2.seat).thenReturn(seat2)
        `when`(seat1.seatPrice).thenReturn(10000)
        `when`(seat2.seatPrice).thenReturn(20000)

        `when`(paymentRepository.save(PAYMENT1))
            .thenThrow(RuntimeException("Payment failed"))
            .thenAnswer { it.arguments[0] }

        // When
        val result = paymentManager.executeAndSaveHistory(user, listOf(reservation1, reservation2))

        // Then
        assertEquals(2, result.size)
    }

    companion object {
        val RESERVATION1 =
            Reservation(
                user = User("user1"),
                seat = ReservationManagerTest.SEAT1,
                concertTitle = "concert1",
                concertAt = LocalDateTime.now().plusDays(5),
                reservationStatus = ReservationStatus.PAYMENT_PENDING,
                createdAt = LocalDateTime.now().minusMinutes(10),
            ).apply {
                val field = Reservation::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(this, 1L)
            }
        val RESERVATION2 =
            Reservation(
                user = User("user2"),
                seat = ReservationManagerTest.SEAT2,
                concertTitle = "concert2",
                concertAt = LocalDateTime.now().plusDays(5),
                reservationStatus = ReservationStatus.PAYMENT_PENDING,
                createdAt = LocalDateTime.now().minusMinutes(10),
            ).apply {
                val field = Reservation::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(this, 2L)
            }
        val PAYMENT1 =
            Payment(
                user = User("user1"),
                reservation = RESERVATION1,
                amount = RESERVATION1.seat.seatPrice,
                executedAt = LocalDateTime.now(),
                paymentStatus = PaymentStatus.COMPLETED,
            ).apply {
                val field = Payment::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(this, 1L)
            }
    }
}
