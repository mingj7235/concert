package com.hhplus.concert.application.facade.unittest

import com.hhplus.concert.business.application.dto.ReservationServiceDto
import com.hhplus.concert.business.application.service.ReservationService
import com.hhplus.concert.business.domain.entity.Concert
import com.hhplus.concert.business.domain.entity.ConcertSchedule
import com.hhplus.concert.business.domain.entity.Queue
import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.manager.ConcertManager
import com.hhplus.concert.business.domain.manager.QueueManager
import com.hhplus.concert.business.domain.manager.ReservationManager
import com.hhplus.concert.business.domain.manager.UserManager
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.domain.manager.reservation.ReservationManagerTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.LocalDateTime
import kotlin.test.Test

/**
 * ReservationService 테스트
 */
class ReservationServiceTest {
    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var queueManager: QueueManager

    @Mock
    private lateinit var concertManager: ConcertManager

    @Mock
    private lateinit var reservationManager: ReservationManager

    @InjectMocks
    private lateinit var reservationService: ReservationService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `예약 생성 성공 테스트`() {
        // Given
        val token = "test-token"
        val request = ReservationServiceDto.Request(userId = 1L, concertId = 1L, scheduleId = 1L, seatIds = listOf(1L, 2L))
        val user = User("User")
        val queue =
            Queue(
                user = user,
                token = TOKEN,
                joinedAt = LocalDateTime.now(),
                QueueStatus.PROCESSING,
            )
        val availableSeats = listOf(SEAT1, SEAT2)
        val createdReservations =
            listOf(
                RESERVATION1,
                RESERVATION2,
            )

        `when`(queueManager.findByToken(token)).thenReturn(queue)
        `when`(userManager.findById(request.userId)).thenReturn(user)
        `when`(concertManager.getAvailableSeats(request.concertId, request.scheduleId)).thenReturn(availableSeats)
        `when`(reservationManager.createReservations(request)).thenReturn(createdReservations)

        // When
        val result = reservationService.createReservations(token, request)

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].reservationId)
        assertEquals(2L, result[1].reservationId)
        verify(queueManager).findByToken(token)
        verify(userManager).findById(request.userId)
        verify(concertManager).getAvailableSeats(request.concertId, request.scheduleId)
        verify(reservationManager).createReservations(request)
    }

    @Test
    fun `큐 상태가 Processing이 아닐 때 예외 발생 테스트`() {
        // Given
        val request = ReservationServiceDto.Request(userId = 1L, concertId = 1L, scheduleId = 1L, seatIds = listOf(1L))
        val user = User("User")
        val queue =
            Queue(
                user = user,
                token = TOKEN,
                joinedAt = LocalDateTime.now(),
                QueueStatus.WAITING,
            )

        `when`(queueManager.findByToken(TOKEN)).thenReturn(queue)

        // When & Then
        assertThrows<BusinessException.BadRequest> {
            reservationService.createReservations(TOKEN, request)
        }
    }

    @Test
    fun `요청한 좌석이 사용 불가능할 때 예외 발생 테스트`() {
        // Given
        val request = ReservationServiceDto.Request(userId = 1L, concertId = 1L, scheduleId = 1L, seatIds = listOf(1L, 3L))
        val user = User("User")
        val queue =
            Queue(
                user = user,
                token = TOKEN,
                joinedAt = LocalDateTime.now(),
                QueueStatus.PROCESSING,
            )
        val availableSeats = listOf(SEAT1)

        `when`(queueManager.findByToken(TOKEN)).thenReturn(queue)
        `when`(userManager.findById(request.userId)).thenReturn(user)
        `when`(concertManager.getAvailableSeats(request.concertId, request.scheduleId)).thenReturn(availableSeats)

        // When & Then
        assertThrows<BusinessException.BadRequest> {
            reservationService.createReservations(TOKEN, request)
        }
    }

    companion object {
        const val TOKEN = "test_token"
        val AVAILABLE_CONCERT =
            Concert(
                title = "availableConcert",
                description = "available",
                concertStatus = ConcertStatus.AVAILABLE,
            )

        val VALID_SCHEDULE =
            ConcertSchedule(
                AVAILABLE_CONCERT,
                concertAt = LocalDateTime.now().plusDays(2),
                reservationAvailableAt = LocalDateTime.now().minusDays(1),
            )

        val SEAT1 =
            Seat(
                concertSchedule = VALID_SCHEDULE,
                seatNumber = 1,
                seatStatus = SeatStatus.AVAILABLE,
                seatPrice = 10000,
            ).apply {
                val field = Seat::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(this, 1L)
            }
        val SEAT2 =
            Seat(
                concertSchedule = VALID_SCHEDULE,
                seatNumber = 2,
                seatStatus = SeatStatus.AVAILABLE,
                seatPrice = 10000,
            ).apply {
                val field = Seat::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(this, 2L)
            }
        val SEAT3 =
            Seat(
                concertSchedule = VALID_SCHEDULE,
                seatNumber = 2,
                seatStatus = SeatStatus.UNAVAILABLE,
                seatPrice = 10000,
            ).apply {
                val field = Seat::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(this, 3L)
            }
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
    }
}
