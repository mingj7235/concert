package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.service.ConcertService
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.QueueRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.infrastructure.entity.Concert
import com.hhplus.concert.infrastructure.entity.ConcertSchedule
import com.hhplus.concert.infrastructure.entity.Queue
import com.hhplus.concert.infrastructure.entity.Seat
import com.hhplus.concert.infrastructure.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@Transactional
class ConcertServiceIntegrationTest {
    @Autowired
    private lateinit var concertService: ConcertService

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

    @Nested
    @DisplayName("[getAvailableConcerts] 테스트")
    inner class GetAvailableConcertsTest {
        @Test
        fun `PROCESSING 상태의 큐를 가진 사용자에게 예약 가능한 콘서트 목록을 반환해야 한다`() {
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

            val availableConcert =
                concertRepository.save(
                    Concert(
                        title = "Available Concert",
                        description = "This is an available concert",
                        concertStatus = ConcertStatus.AVAILABLE,
                    ),
                )
            concertRepository.save(
                Concert(
                    title = "Unavailable Concert",
                    description = "This is an unavailable concert",
                    concertStatus = ConcertStatus.UNAVAILABLE,
                ),
            )

            // when
            val result = concertService.getAvailableConcerts(token)

            // then
            assertEquals(1, result.size)
            assertEquals(availableConcert.id, result[0].concertId)
            assertEquals(availableConcert.title, result[0].title)
            assertEquals(availableConcert.description, result[0].description)
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

            // when & then
            assertThrows<BusinessException.BadRequest> {
                concertService.getAvailableConcerts(token)
            }
        }

        @Test
        fun `존재하지 않는 토큰으로 요청시 예외를 발생시켜야 한다`() {
            // given
            val nonExistentToken = "non_existent_token"

            // when & then
            assertThrows<BusinessException.NotFound> {
                concertService.getAvailableConcerts(nonExistentToken)
            }
        }

        @Test
        fun `예약 가능한 콘서트가 없을 때 빈 리스트를 반환해야 한다`() {
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

            // when
            val result = concertService.getAvailableConcerts(token)

            // then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `여러 개의 예약 가능한 콘서트가 있을 때 모두 반환해야 한다`() {
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

            val availableConcerts =
                listOf(
                    Concert(title = "Concert 1", description = "Description 1", concertStatus = ConcertStatus.AVAILABLE),
                    Concert(title = "Concert 2", description = "Description 2", concertStatus = ConcertStatus.AVAILABLE),
                    Concert(title = "Concert 3", description = "Description 3", concertStatus = ConcertStatus.AVAILABLE),
                )
            concertRepository.saveAll(availableConcerts)

            // when
            val result = concertService.getAvailableConcerts(token)

            // then
            assertEquals(3, result.size)
            result.forEachIndexed { index, concert ->
                assertEquals(availableConcerts[index].id, concert.concertId)
                assertEquals(availableConcerts[index].title, concert.title)
                assertEquals(availableConcerts[index].description, concert.description)
            }
        }
    }

    @Nested
    @DisplayName("[getConcertSchedules] 테스트")
    inner class GetConcertSchedulesTest {
        @Test
        fun `PROCESSING 상태의 큐를 가진 사용자에게 예약 가능한 콘서트 스케줄 목록을 반환해야 한다`() {
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
                        title = "Available Concert",
                        description = "This is an available concert",
                        concertStatus = ConcertStatus.AVAILABLE,
                    ),
                )

            val schedule1 =
                concertScheduleRepository.save(
                    ConcertSchedule(
                        concert = concert,
                        concertAt = LocalDateTime.now().plusDays(1),
                        reservationAvailableAt = LocalDateTime.now().minusHours(1),
                    ),
                )
            val schedule2 =
                concertScheduleRepository.save(
                    ConcertSchedule(
                        concert = concert,
                        concertAt = LocalDateTime.now().plusDays(2),
                        reservationAvailableAt = LocalDateTime.now().minusHours(1),
                    ),
                )

            // when
            val result = concertService.getConcertSchedules(token, concert.id)

            // then
            assertEquals(concert.id, result.concertId)
            assertEquals(2, result.events.size)
            assertEquals(schedule1.id, result.events[0].scheduleId)
            assertEquals(schedule1.concertAt, result.events[0].concertAt)
            assertEquals(schedule1.reservationAvailableAt, result.events[0].reservationAt)
            assertEquals(schedule2.id, result.events[1].scheduleId)
            assertEquals(schedule2.concertAt, result.events[1].concertAt)
            assertEquals(schedule2.reservationAvailableAt, result.events[1].reservationAt)
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
            val concertId = 1L

            // when & then
            assertThrows<BusinessException.BadRequest> {
                concertService.getConcertSchedules(token, concertId)
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

            // when & then
            assertThrows<BusinessException.NotFound> {
                concertService.getConcertSchedules(token, nonExistentConcertId)
            }
        }

        @Test
        fun `예약 불가능한 콘서트에 대해 예외를 발생시켜야 한다`() {
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

            val unavailableConcert =
                concertRepository.save(
                    Concert(
                        title = "Unavailable Concert",
                        description = "This is an unavailable concert",
                        concertStatus = ConcertStatus.UNAVAILABLE,
                    ),
                )

            // when & then
            assertThrows<BusinessException.BadRequest> {
                concertService.getConcertSchedules(token, unavailableConcert.id)
            }
        }

        @Test
        fun `예약 가능한 시간이 지난 스케줄은 반환하지 않아야 한다`() {
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
                        title = "Available Concert",
                        description = "This is an available concert",
                        concertStatus = ConcertStatus.AVAILABLE,
                    ),
                )

            val validSchedule =
                concertScheduleRepository.save(
                    ConcertSchedule(
                        concert = concert,
                        concertAt = LocalDateTime.now().plusDays(1),
                        reservationAvailableAt = LocalDateTime.now().minusHours(1),
                    ),
                )
            concertScheduleRepository.save(
                ConcertSchedule(
                    concert = concert,
                    concertAt = LocalDateTime.now().minusDays(1),
                    reservationAvailableAt = LocalDateTime.now().minusDays(2),
                ),
            )

            // when
            val result = concertService.getConcertSchedules(token, concert.id)

            // then
            assertEquals(concert.id, result.concertId)
            assertEquals(1, result.events.size)
            assertEquals(validSchedule.id, result.events[0].scheduleId)
        }
    }

    @Nested
    @DisplayName("[getAvailableSeats] 테스트")
    inner class GetAvailableSeats {
        @Test
        fun `PROCESSING 상태의 큐를 가진 사용자에게 예약 가능한 좌석 목록을 반환해야 한다`() {
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
                        title = "Available Concert",
                        description = "This is an available concert",
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

            // when
            val result = concertService.getAvailableSeats(token, concert.id, schedule.id)

            // then
            assertEquals(concert.id, result.concertId)
            assertEquals(2, result.seats.size)
            assertEquals(seat1.id, result.seats[0].seatId)
            assertEquals(seat1.seatStatus, result.seats[0].seatStatus)
            assertEquals(seat1.seatNumber, result.seats[0].seatNumber)
            assertEquals(seat1.seatPrice, result.seats[0].seatPrice)
            assertEquals(seat2.id, result.seats[1].seatId)
            assertEquals(seat2.seatStatus, result.seats[1].seatStatus)
            assertEquals(seat2.seatNumber, result.seats[1].seatNumber)
            assertEquals(seat2.seatPrice, result.seats[1].seatPrice)
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
            val concertId = 1L
            val scheduleId = 1L

            // when & then
            assertThrows<BusinessException.BadRequest> {
                concertService.getAvailableSeats(token, concertId, scheduleId)
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
            val scheduleId = 1L

            // when & then
            assertThrows<BusinessException.NotFound> {
                concertService.getAvailableSeats(token, nonExistentConcertId, scheduleId)
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
                        title = "Available Concert",
                        description = "This is an available concert",
                        concertStatus = ConcertStatus.AVAILABLE,
                    ),
                )
            val nonExistentScheduleId = 99999L

            // when & then
            assertThrows<BusinessException.NotFound> {
                concertService.getAvailableSeats(token, concert.id, nonExistentScheduleId)
            }
        }

        @Test
        fun `예약 불가능한 콘서트에 대해 예외를 발생시켜야 한다`() {
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

            val unavailableConcert =
                concertRepository.save(
                    Concert(
                        title = "Unavailable Concert",
                        description = "This is an unavailable concert",
                        concertStatus = ConcertStatus.UNAVAILABLE,
                    ),
                )

            val schedule =
                concertScheduleRepository.save(
                    ConcertSchedule(
                        concert = unavailableConcert,
                        concertAt = LocalDateTime.now().plusDays(1),
                        reservationAvailableAt = LocalDateTime.now().minusHours(1),
                    ),
                )

            // when & then
            assertThrows<BusinessException.BadRequest> {
                concertService.getAvailableSeats(token, unavailableConcert.id, schedule.id)
            }
        }

        @Test
        fun `예약 가능한 시간이 지난 스케줄에 대해 예외를 발생시켜야 한다`() {
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
                        title = "Available Concert",
                        description = "This is an available concert",
                        concertStatus = ConcertStatus.AVAILABLE,
                    ),
                )

            val pastSchedule =
                concertScheduleRepository.save(
                    ConcertSchedule(
                        concert = concert,
                        concertAt = LocalDateTime.now().minusDays(1),
                        reservationAvailableAt = LocalDateTime.now().minusDays(2),
                    ),
                )

            // when & then
            assertThrows<BusinessException.BadRequest> {
                concertService.getAvailableSeats(token, concert.id, pastSchedule.id)
            }
        }
    }
}
