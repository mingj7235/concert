package com.hhplus.concert.application.facade

import com.hhplus.concert.business.application.service.ConcertService
import com.hhplus.concert.business.domain.manager.concert.ConcertManager
import com.hhplus.concert.business.domain.manager.queue.QueueManager
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.domain.manager.concert.ConcertManagerTest.Companion.AVAILABLE_CONCERT
import com.hhplus.concert.domain.manager.queue.QueueManagerTest
import com.hhplus.concert.infrastructure.entity.Concert
import com.hhplus.concert.infrastructure.entity.ConcertSchedule
import com.hhplus.concert.infrastructure.entity.Queue
import com.hhplus.concert.infrastructure.entity.Seat
import com.hhplus.concert.infrastructure.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.LocalDateTime

/**
 * ConcertService 단위 테스트
 */
class ConcertServiceTest {
    @Mock
    private lateinit var concertManager: ConcertManager

    @Mock
    private lateinit var queueManager: QueueManager

    @InjectMocks
    private lateinit var concertService: ConcertService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `유효한 토큰으로 예약 가능한 콘서트 목록을 반환해야 한다`() {
        val token = TOKEN
        val queue = PROCESSING_QUEUE
        val concert1 = AVAILABLE_CONCERT1
        val concert2 = AVAILABLE_CONCERT2

        `when`(queueManager.findByToken(token)).thenReturn(queue)
        `when`(concertManager.getAvailableConcerts()).thenReturn(listOf(concert1, concert2))

        val result = concertService.getAvailableConcerts(token)

        assertThat(result.size).isEqualTo(2)
        assert(result[0].title == "availableConcert1")
        assert(result[0].description == "available1")
        assert(result[1].title == "availableConcert2")
        assert(result[1].description == "available2")
    }

    @Test
    fun `유효하지 않은 큐 상태로 콘서트 목록 조회 시 예외를 발생시켜야 한다`() {
        val token = TOKEN
        val queue = WAITING_QUEUE

        `when`(queueManager.findByToken(token)).thenReturn(queue)

        assertThrows<BusinessException.BadRequest> {
            concertService.getAvailableConcerts(token)
        }
    }

    @Test
    fun `유효한 토큰으로 콘서트 스케줄을 반환해야 한다`() {
        val token = TOKEN
        val concertId = 0L
        val queue = PROCESSING_QUEUE
        val schedule1 = VALID_SCHEDULE1
        val schedule2 = VALID_SCHEDULE2

        `when`(queueManager.findByToken(token)).thenReturn(queue)
        `when`(concertManager.getAvailableConcertSchedules(concertId)).thenReturn(listOf(schedule1, schedule2))

        val result = concertService.getConcertSchedules(token, concertId)

        assert(result.concertId == concertId)
        assert(result.events.size == 2)
    }

    @Test
    fun `유효한 토큰으로 좌석 목록을 반환해야 한다`() {
        val token = TOKEN
        val concertId = 0L
        val scheduleId = 0L
        val queue = PROCESSING_QUEUE
        val seat1 = SEAT1
        val seat2 = SEAT2

        `when`(queueManager.findByToken(token)).thenReturn(queue)
        `when`(concertManager.getAvailableSeats(concertId, scheduleId)).thenReturn(listOf(seat1, seat2))

        val result = concertService.getAvailableSeats(token, concertId, scheduleId)

        assert(result.concertId == concertId)
        assert(result.seats.size == 2)
        assert(result.seats[0].seatPrice == 10000)
        assert(result.seats[0].seatNumber == 1)
        assert(result.seats[0].seatStatus == SeatStatus.AVAILABLE)
        assert(result.seats[1].seatPrice == 20000)
        assert(result.seats[1].seatNumber == 2)
        assert(result.seats[1].seatStatus == SeatStatus.AVAILABLE)
    }

    @Test
    fun `유효하지 않은 큐 상태로 좌석 목록 조회 시 예외를 발생시켜야 한다`() {
        val token = "invalidToken"
        val concertId = 1L
        val scheduleId = 1L
        val queue = WAITING_QUEUE

        `when`(queueManager.findByToken(token)).thenReturn(queue)

        assertThrows<BusinessException.BadRequest> {
            concertService.getAvailableSeats(token, concertId, scheduleId)
        }
    }

    companion object {
        const val TOKEN = "test_token"
        val PROCESSING_QUEUE =
            Queue(
                user = User("User"),
                token = QueueManagerTest.TOKEN,
                joinedAt = LocalDateTime.now(),
                QueueStatus.PROCESSING,
            )

        val WAITING_QUEUE =
            Queue(
                user = User("User"),
                token = QueueManagerTest.TOKEN,
                joinedAt = LocalDateTime.now(),
                QueueStatus.WAITING,
            )

        val AVAILABLE_CONCERT1 =
            Concert(
                title = "availableConcert1",
                description = "available1",
                concertStatus = ConcertStatus.AVAILABLE,
            )

        val AVAILABLE_CONCERT2 =
            Concert(
                title = "availableConcert2",
                description = "available2",
                concertStatus = ConcertStatus.AVAILABLE,
            )

        val VALID_SCHEDULE1 =
            ConcertSchedule(
                AVAILABLE_CONCERT,
                concertAt = LocalDateTime.now().plusDays(2),
                reservationAvailableAt = LocalDateTime.now().minusDays(1),
            )
        val VALID_SCHEDULE2 =
            ConcertSchedule(
                AVAILABLE_CONCERT,
                concertAt = LocalDateTime.now().plusDays(5),
                reservationAvailableAt = LocalDateTime.now().minusDays(2),
            )
        val SEAT1 =
            Seat(
                concertSchedule = VALID_SCHEDULE1,
                seatNumber = 1,
                seatStatus = SeatStatus.AVAILABLE,
                seatPrice = 10000,
            )
        val SEAT2 =
            Seat(
                concertSchedule = VALID_SCHEDULE1,
                seatNumber = 2,
                seatStatus = SeatStatus.AVAILABLE,
                seatPrice = 20000,
            )
    }
}
