package com.hhplus.concert.application.facade.integration

import com.hhplus.concert.business.application.dto.ReservationServiceDto
import com.hhplus.concert.business.application.service.ReservationService
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
import com.hhplus.concert.common.error.exception.BusinessException
import com.hhplus.concert.common.type.ConcertStatus
import com.hhplus.concert.common.type.QueueStatus
import com.hhplus.concert.common.type.SeatStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertTrue

@SpringBootTest
class ReservationServiceConcurrencyTest {
    @Autowired
    private lateinit var reservationService: ReservationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var concertRepository: ConcertRepository

    @Autowired
    private lateinit var concertScheduleRepository: ConcertScheduleRepository

    @Autowired
    private lateinit var seatRepository: SeatRepository

    @Autowired
    private lateinit var queueRepository: QueueRepository

    @Test
    fun `1000개의 동시 예약 요청 중 하나만 성공해야 한다`() {
        // Given

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
                Seat(schedule, 1, SeatStatus.AVAILABLE, 10000),
            )

        val threadCount = 1000
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        val successfulReservations = mutableListOf<ReservationServiceDto.Result>()
        val failedReservations = mutableListOf<Throwable>()

        // When
        repeat(threadCount) { index ->
            executorService.submit {
                try {
                    val token = "test_token_$index"
                    val user = userRepository.save(User(name = "Test User$index"))
                    queueRepository.save(
                        Queue(
                            user = user,
                            token = token,
                            joinedAt = LocalDateTime.now(),
                            queueStatus = QueueStatus.PROCESSING,
                        ),
                    )

                    val reservationRequest =
                        ReservationServiceDto.Request(
                            userId = user.id,
                            concertId = concert.id,
                            scheduleId = schedule.id,
                            seatIds = listOf(seat.id),
                        )

                    val result = reservationService.createReservations(token, reservationRequest)
                    synchronized(successfulReservations) {
                        successfulReservations.addAll(result)
                    }
                } catch (e: Exception) {
                    synchronized(failedReservations) {
                        failedReservations.add(e)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        // Then
        assertEquals(1, successfulReservations.size, "1개의 예약만 성공해야 합니다.")
        assertEquals(999, failedReservations.size, "999개의 예약은 실패해야 합니다.")
        assertTrue(failedReservations.all { it is BusinessException.BadRequest }, "실패한 예약들은 모두 BusinessException.Conflict 예외여야 합니다.")

        val updatedSeat = seatRepository.findById(seat.id)!!
        assertEquals(SeatStatus.UNAVAILABLE, updatedSeat.seatStatus, "좌석 상태가 UNAVAILABLE로 변경되어야 합니다.")
    }
}
