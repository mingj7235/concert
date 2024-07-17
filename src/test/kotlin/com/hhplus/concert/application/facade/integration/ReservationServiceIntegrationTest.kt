package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.dto.ReservationServiceDto
import com.hhplus.concert.business.application.service.ReservationService
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.QueueRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.type.ReservationStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.infrastructure.entity.Concert
import com.hhplus.concert.infrastructure.entity.ConcertSchedule
import com.hhplus.concert.infrastructure.entity.Queue
import com.hhplus.concert.infrastructure.entity.Seat
import com.hhplus.concert.infrastructure.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@Transactional
class ReservationServiceIntegrationTest {
    @Autowired
    private lateinit var reservationService: ReservationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var queueRepository: QueueRepository

    @Autowired
    private lateinit var concertRepository: ConcertRepository

    @Autowired
    private lateinit var concertScheduleRepository: ConcertScheduleRepository

    @Autowired
    private lateinit var seatRepository: SeatRepository

    @Test
    fun `유효한 요청으로 좌석 예약에 성공해야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = "test_token"
        queueRepository.save(
            Queue(
                user = user,
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.PROCESSING,
            ),
        )

        val concert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )
        val schedule =
            concertScheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )
        val seat =
            seatRepository.save(
                Seat(
                    concertSchedule = schedule,
                    seatStatus = SeatStatus.AVAILABLE,
                    seatNumber = 1,
                    seatPrice = 10000,
                ),
            )

        val reservationRequest =
            ReservationServiceDto.Request(
                userId = user.id,
                concertId = concert.id,
                scheduleId = schedule.id,
                seatIds = listOf(seat.id),
            )

        // when
        val result = reservationService.createReservations(token, reservationRequest)

        // then
        assertEquals(1, result.size)
        assertEquals(concert.id, result[0].concertId)
        assertEquals(concert.title, result[0].concertName)
        assertEquals(schedule.concertAt, result[0].concertAt)
        assertEquals(seat.seatNumber, result[0].seat.seatNumber)
        assertEquals(seat.seatPrice, result[0].seat.price)
        assertEquals(ReservationStatus.PAYMENT_PENDING, result[0].reservationStatus)

        val updatedSeat = seatRepository.findById(seat.id)
        assertEquals(SeatStatus.UNAVAILABLE, updatedSeat!!.seatStatus)
    }

    @Test
    fun `PROCESSING 상태가 아닌 큐를 가진 사용자의 요청에 대해 예외를 발생시켜야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = "test_token"
        queueRepository.save(
            Queue(
                user = user,
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.WAITING,
            ),
        )

        val reservationRequest =
            ReservationServiceDto.Request(
                userId = user.id,
                concertId = 1L,
                scheduleId = 1L,
                seatIds = listOf(1L),
            )

        // when & then
        assertThrows<BusinessException.BadRequest> {
            reservationService.createReservations(token, reservationRequest)
        }
    }

    @Test
    fun `존재하지 않는 사용자 ID로 요청시 예외를 발생시켜야 한다`() {
        // given
        val token = "test_token"
        queueRepository.save(
            Queue(
                user = userRepository.save(User(name = "Test User")),
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.PROCESSING,
            ),
        )

        val nonExistentUserId = 99999L
        val reservationRequest =
            ReservationServiceDto.Request(
                userId = nonExistentUserId,
                concertId = 1L,
                scheduleId = 1L,
                seatIds = listOf(1L),
            )

        // when & then
        assertThrows<BusinessException.NotFound> {
            reservationService.createReservations(token, reservationRequest)
        }
    }

    @Test
    fun `존재하지 않는 콘서트 ID로 요청시 예외를 발생시켜야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = "test_token"
        queueRepository.save(
            Queue(
                user = user,
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.PROCESSING,
            ),
        )

        val nonExistentConcertId = 99999L
        val reservationRequest =
            ReservationServiceDto.Request(
                userId = user.id,
                concertId = nonExistentConcertId,
                scheduleId = 1L,
                seatIds = listOf(1L),
            )

        // when & then
        assertThrows<BusinessException.NotFound> {
            reservationService.createReservations(token, reservationRequest)
        }
    }

    @Test
    fun `존재하지 않는 스케줄 ID로 요청시 예외를 발생시켜야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = "test_token"
        queueRepository.save(
            Queue(
                user = user,
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.PROCESSING,
            ),
        )

        val concert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )

        val nonExistentScheduleId = 99999L
        val reservationRequest =
            ReservationServiceDto.Request(
                userId = user.id,
                concertId = concert.id,
                scheduleId = nonExistentScheduleId,
                seatIds = listOf(1L),
            )

        // when & then
        assertThrows<BusinessException.NotFound> {
            reservationService.createReservations(token, reservationRequest)
        }
    }

    @Test
    fun `이미 예약된 좌석을 요청시 예외를 발생시켜야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = "test_token"
        queueRepository.save(
            Queue(
                user = user,
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.PROCESSING,
            ),
        )

        val concert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )
        val schedule =
            concertScheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )
        val seat =
            seatRepository.save(
                Seat(
                    concertSchedule = schedule,
                    seatStatus = SeatStatus.UNAVAILABLE,
                    seatNumber = 1,
                    seatPrice = 10000,
                ),
            )

        val reservationRequest =
            ReservationServiceDto.Request(
                userId = user.id,
                concertId = concert.id,
                scheduleId = schedule.id,
                seatIds = listOf(seat.id),
            )

        // when & then
        assertThrows<BusinessException.BadRequest> {
            reservationService.createReservations(token, reservationRequest)
        }
    }

    @Test
    fun `존재하지 않는 좌석 ID로 요청시 예외를 발생시켜야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = "test_token"
        queueRepository.save(
            Queue(
                user = user,
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.PROCESSING,
            ),
        )

        val concert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )
        val schedule =
            concertScheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )

        val nonExistentSeatId = 99999L
        val reservationRequest =
            ReservationServiceDto.Request(
                userId = user.id,
                concertId = concert.id,
                scheduleId = schedule.id,
                seatIds = listOf(nonExistentSeatId),
            )

        // when & then
        assertThrows<BusinessException.BadRequest> {
            reservationService.createReservations(token, reservationRequest)
        }
    }

    @Test
    fun `여러 좌석을 한 번에 예약할 수 있어야 한다`() {
        // given
        val user = userRepository.save(User(name = "Test User"))
        val token = "test_token"
        queueRepository.save(
            Queue(
                user = user,
                token = token,
                joinedAt = LocalDateTime.now(),
                queueStatus = QueueStatus.PROCESSING,
            ),
        )

        val concert =
            concertRepository.save(
                Concert(
                    title = "Test Concert",
                    description = "Test Description",
                    concertStatus = ConcertStatus.AVAILABLE,
                ),
            )
        val schedule =
            concertScheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().plusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusHours(1),
                ),
            )
        val seat1 =
            seatRepository.save(
                Seat(
                    concertSchedule = schedule,
                    seatStatus = SeatStatus.AVAILABLE,
                    seatNumber = 1,
                    seatPrice = 10000,
                ),
            )
        val seat2 =
            seatRepository.save(
                Seat(
                    concertSchedule = schedule,
                    seatStatus = SeatStatus.AVAILABLE,
                    seatNumber = 2,
                    seatPrice = 20000,
                ),
            )

        val reservationRequest =
            ReservationServiceDto.Request(
                userId = user.id,
                concertId = concert.id,
                scheduleId = schedule.id,
                seatIds = listOf(seat1.id, seat2.id),
            )

        // when
        val result = reservationService.createReservations(token, reservationRequest)

        // then
        assertEquals(2, result.size)
        assertTrue(result.any { it.seat.seatNumber == 1 })
        assertTrue(result.any { it.seat.seatNumber == 2 })
        result.forEach { assertEquals(ReservationStatus.PAYMENT_PENDING, it.reservationStatus) }

        val updatedSeats = seatRepository.findAllById(listOf(seat1.id, seat2.id))
        updatedSeats.forEach { assertEquals(SeatStatus.UNAVAILABLE, it.seatStatus) }
    }
}
