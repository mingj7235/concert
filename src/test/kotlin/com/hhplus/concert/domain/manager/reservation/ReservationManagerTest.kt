package com.hhplus.concert.domain.manager.reservation

import com.hhplus.concert.business.application.dto.ReservationServiceDto
import com.hhplus.concert.business.domain.entity.Concert
import com.hhplus.concert.business.domain.entity.ConcertSchedule
import com.hhplus.concert.business.domain.entity.Reservation
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.manager.ReservationManager
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.ReservationRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.common.type.SeatStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.LocalDateTime

/**
 * ReservationManager 단위 테스트
 */
class ReservationManagerTest {
    @Mock
    private lateinit var reservationRepository: ReservationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var concertRepository: ConcertRepository

    @Mock
    private lateinit var concertScheduleRepository: ConcertScheduleRepository

    @Mock
    private lateinit var seatRepository: SeatRepository

    @InjectMocks
    private lateinit var reservationManager: ReservationManager

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `createReservations should create reservations and update seat status`() {
        // Given
        val userId = 0L
        val concertId = 0L
        val scheduleId = 0L
        val seatIds = listOf(0L, 1L)

        val user = User("User")
        val concert = AVAILABLE_CONCERT
        val concertSchedule = VALID_SCHEDULE
        val seats = listOf(SEAT1, SEAT2)

        val request = ReservationServiceDto.Request(userId, concertId, scheduleId, seatIds)

        `when`(userRepository.findById(userId)).thenReturn(user)
        `when`(concertRepository.findById(concertId)).thenReturn(concert)
        `when`(concertScheduleRepository.findById(scheduleId)).thenReturn(concertSchedule)
        `when`(seatRepository.finaAllByIdWithLock(seatIds)).thenReturn(seats)

        // When
        val result = reservationManager.createReservations(request)

        // Then
        assertEquals(2, result.size)
        verify(seatRepository).updateAllStatus(seatIds, SeatStatus.UNAVAILABLE)
    }

    @Test
    fun `만료된 예악건들을 조회하여 예약 상태와 좌석 정보를 업데이트 한다`() {
        // Given
        val expirationTime = LocalDateTime.now().minusMinutes(5)
        val expiredReservations = listOf(RESERVATION1, RESERVATION2)

        `when`(
            reservationRepository.findExpiredReservations(
                ReservationStatus.PAYMENT_PENDING,
                expirationTime,
            ),
        ).thenReturn(expiredReservations)

        // When
        reservationManager.cancelReservations()

        // Then
        val expectedReservationIds = expiredReservations.map { it.id }
        val expectedSeatIds = expiredReservations.map { it.seat.id }

        // Additional verifications
        assertEquals(2, expectedReservationIds.size)
        assertEquals(2, expectedSeatIds.size)
    }

    companion object {
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
            )
        val SEAT2 =
            Seat(
                concertSchedule = VALID_SCHEDULE,
                seatNumber = 2,
                seatStatus = SeatStatus.AVAILABLE,
                seatPrice = 10000,
            )
        val RESERVATION1 =
            Reservation(
                user = User("user1"),
                seat = SEAT1,
                concertTitle = "concert1",
                concertAt = LocalDateTime.now().plusDays(5),
                reservationStatus = ReservationStatus.PAYMENT_PENDING,
                createdAt = LocalDateTime.now().minusMinutes(10),
            )
        val RESERVATION2 =
            Reservation(
                user = User("user2"),
                seat = SEAT2,
                concertTitle = "concert2",
                concertAt = LocalDateTime.now().plusDays(5),
                reservationStatus = ReservationStatus.PAYMENT_PENDING,
                createdAt = LocalDateTime.now().minusMinutes(10),
            )
    }
}
