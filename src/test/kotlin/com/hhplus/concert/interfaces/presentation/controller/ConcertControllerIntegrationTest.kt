package com.hhplus.concert.interfaces.presentation.controller

import com.hhplus.concert.business.domain.entity.Concert
import com.hhplus.concert.business.domain.entity.ConcertSchedule
import com.hhplus.concert.business.domain.entity.Queue
import com.hhplus.concert.business.domain.entity.Seat
import com.hhplus.concert.business.domain.entity.User
import com.hhplus.concert.business.domain.repository.ConcertRepository
import com.hhplus.concert.business.domain.repository.ConcertScheduleRepository
import com.hhplus.concert.business.domain.repository.QueueRepository
import com.hhplus.concert.business.domain.repository.SeatRepository
import com.hhplus.concert.business.domain.repository.UserRepository
import com.hhplus.concert.common.constants.TokenConstants.QUEUE_TOKEN_HEADER
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.type.SeatStatus
import com.hhplus.concert.common.util.JwtUtil
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConcertControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

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

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Nested
    @DisplayName("[getAvailableConcerts] 테스트")
    inner class GetAvailableConcertsTest {
        @Test
        fun `유효한 토큰으로 예약 가능한 콘서트 목록을 조회해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
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
            val result =
                mockMvc.get("/api/v1/concerts") {
                    header(QUEUE_TOKEN_HEADER, token)
                }

            // then
            result.andExpect {
                status { isOk() }
                jsonPath("$[0].concertId").value(availableConcert.id)
                jsonPath("$[0].title").value(availableConcert.title)
                jsonPath("$[0].description").value(availableConcert.description)
                jsonPath("$[1]").doesNotExist()
            }
        }

        @Test
        fun `유효하지 않은 토큰으로 요청시 인증 에러를 반환해야 한다`() {
            // given
            val invalidToken = "invalid_token"

            // when & then
            mockMvc
                .get("/api/v1/concerts") {
                    header(QUEUE_TOKEN_HEADER, invalidToken)
                }.andExpect {
                    status { isUnauthorized() }
                }
        }

        @Test
        fun `PROCESSING 상태가 아닌 큐를 가진 사용자의 요청에 대해 에러를 반환해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
            queueRepository.save(
                Queue(
                    user = user,
                    token = token,
                    joinedAt = LocalDateTime.now(),
                    queueStatus = QueueStatus.WAITING,
                ),
            )

            // when & then
            mockMvc
                .get("/api/v1/concerts") {
                    header(QUEUE_TOKEN_HEADER, token)
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `예약 가능한 콘서트가 없을 때 빈 리스트를 반환해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
            queueRepository.save(
                Queue(
                    user = user,
                    token = token,
                    joinedAt = LocalDateTime.now(),
                    queueStatus = QueueStatus.PROCESSING,
                ),
            )

            // when
            val result =
                mockMvc.get("/api/v1/concerts") {
                    header(QUEUE_TOKEN_HEADER, token)
                }

            // then
            result.andExpect {
                status { isOk() }
                jsonPath("$").isArray()
                jsonPath("$").isEmpty()
            }
        }
    }

    @Nested
    @DisplayName("[getConcertSchedules] 테스트")
    inner class GetConcertSchedulesTest {
        @Test
        fun `유효한 토큰과 콘서트 ID로 예약 가능한 스케줄 목록을 조회해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
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
            val result =
                mockMvc.get("/api/v1/concerts/${concert.id}/schedules") {
                    header(QUEUE_TOKEN_HEADER, token)
                }

            // then
            result.andExpect {
                status { isOk() }
                jsonPath("$.concertId").value(concert.id)
                jsonPath("$.events").isArray()
                jsonPath("$.events[0].scheduleId").value(schedule1.id)
                jsonPath("$.events[0].concertAt").exists()
                jsonPath("$.events[0].reservationAt").exists()
                jsonPath("$.events[1].scheduleId").value(schedule2.id)
                jsonPath("$.events[1].concertAt").exists()
                jsonPath("$.events[1].reservationAt").exists()
            }
        }

        @Test
        fun `유효하지 않은 토큰으로 요청시 인증 에러를 반환해야 한다`() {
            // given
            val invalidToken = "invalid_token"
            val concertId = 1L

            // when & then
            mockMvc
                .get("/api/v1/concerts/$concertId/schedules") {
                    header(QUEUE_TOKEN_HEADER, invalidToken)
                }.andExpect {
                    status { isUnauthorized() }
                }
        }

        @Test
        fun `PROCESSING 상태가 아닌 큐를 가진 사용자의 요청에 대해 에러를 반환해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
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
            mockMvc
                .get("/api/v1/concerts/$concertId/schedules") {
                    header(QUEUE_TOKEN_HEADER, token)
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `존재하지 않는 콘서트 ID로 요청시 Not Found 에러를 반환해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
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
            mockMvc
                .get("/api/v1/concerts/$nonExistentConcertId/schedules") {
                    header(QUEUE_TOKEN_HEADER, token)
                }.andExpect {
                    status { isNotFound() }
                }
        }
    }

    @Nested
    @DisplayName("[getAvailableSeats] 테스트")
    inner class GetAvailableSeats {
        @Test
        fun `유효한 토큰과 콘서트 ID, 스케줄 ID로 예약 가능한 좌석 목록을 조회해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
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
            val result =
                mockMvc.get("/api/v1/concerts/${concert.id}/schedules/${schedule.id}/seats") {
                    header(QUEUE_TOKEN_HEADER, token)
                }

            // then
            result.andExpect {
                status { isOk() }
                jsonPath("$.concertId").value(concert.id)
                jsonPath("$.seats").isArray()
                jsonPath("$.seats[0].seatId").value(seat1.id)
                jsonPath("$.seats[0].seatStatus").value(seat1.seatStatus.name)
                jsonPath("$.seats[0].seatNumber").value(seat1.seatNumber)
                jsonPath("$.seats[0].seatPrice").value(seat1.seatPrice)
                jsonPath("$.seats[1].seatId").value(seat2.id)
                jsonPath("$.seats[1].seatStatus").value(seat2.seatStatus.name)
                jsonPath("$.seats[1].seatNumber").value(seat2.seatNumber)
                jsonPath("$.seats[1].seatPrice").value(seat2.seatPrice)
            }
        }

        @Test
        fun `유효하지 않은 토큰으로 요청시 인증 에러를 반환해야 한다`() {
            // given
            val invalidToken = "invalid_token"
            val concertId = 1L
            val scheduleId = 1L

            // when & then
            mockMvc
                .get("/api/v1/concerts/$concertId/schedules/$scheduleId/seats") {
                    header(QUEUE_TOKEN_HEADER, invalidToken)
                }.andExpect {
                    status { isUnauthorized() }
                }
        }

        @Test
        fun `PROCESSING 상태가 아닌 큐를 가진 사용자의 요청에 대해 에러를 반환해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
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
            mockMvc
                .get("/api/v1/concerts/$concertId/schedules/$scheduleId/seats") {
                    header(QUEUE_TOKEN_HEADER, token)
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `존재하지 않는 콘서트 ID로 요청시 Not Found 에러를 반환해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
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
            mockMvc
                .get("/api/v1/concerts/$nonExistentConcertId/schedules/$scheduleId/seats") {
                    header(QUEUE_TOKEN_HEADER, token)
                }.andExpect {
                    status { isNotFound() }
                }
        }

        @Test
        fun `존재하지 않는 스케줄 ID로 요청시 Not Found 에러를 반환해야 한다`() {
            // given
            val user = userRepository.save(User(name = "Test User"))
            val token = jwtUtil.generateToken(user.id)
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
            mockMvc
                .get("/api/v1/concerts/${concert.id}/schedules/$nonExistentScheduleId/seats") {
                    header(QUEUE_TOKEN_HEADER, token)
                }.andExpect {
                    status { isNotFound() }
                }
        }
    }
}
